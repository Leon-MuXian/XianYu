package com.serenmeet.auth.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.serenmeet.auth.domain.AdminSessionEntity;
import com.serenmeet.auth.domain.AdminUserEntity;
import com.serenmeet.auth.dto.AdminUserView;
import com.serenmeet.auth.dto.LoginRequest;
import com.serenmeet.auth.dto.LoginResponse;
import com.serenmeet.auth.mapper.AdminSessionMapper;
import com.serenmeet.auth.mapper.AdminUserMapper;
import com.serenmeet.auth.support.AdminLoginRateLimiter;
import com.serenmeet.auth.support.PasswordHasher;
import com.serenmeet.common.ApiException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 平台后台登录和会话校验服务。
 */
@Service
public class AdminAuthApplicationService {

  private final AdminUserMapper adminUserMapper;
  private final AdminSessionMapper adminSessionMapper;
  private final PasswordHasher passwordHasher;
  private final AdminLoginRateLimiter loginRateLimiter;
  private final Clock clock;

  public AdminAuthApplicationService(
    AdminUserMapper adminUserMapper,
    AdminSessionMapper adminSessionMapper,
    PasswordHasher passwordHasher,
    AdminLoginRateLimiter loginRateLimiter,
    Clock clock
  ) {
    this.adminUserMapper = adminUserMapper;
    this.adminSessionMapper = adminSessionMapper;
    this.passwordHasher = passwordHasher;
    this.loginRateLimiter = loginRateLimiter;
    this.clock = clock;
  }

  /**
   * 校验后台账号密码并创建数据库会话。
   */
  @Transactional
  public LoginResponse login(LoginRequest request) {
    loginRateLimiter.checkAllowed(request.username());
    AdminUserEntity user = adminUserMapper.selectOne(
      new LambdaQueryWrapper<AdminUserEntity>().eq(AdminUserEntity::getUsername, request.username())
    );
    if (user == null || !Boolean.TRUE.equals(user.getEnabled()) || !passwordHasher.matches(request.password(), user.getPasswordHash())) {
      loginRateLimiter.recordFailure(request.username());
      throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "账号或密码错误");
    }
    loginRateLimiter.recordSuccess(request.username());

    String token = UUID.randomUUID().toString().replace("-", "");
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDateTime expiresAt = now.plusHours(12);
    AdminSessionEntity session = new AdminSessionEntity();
    session.setToken(token);
    session.setAdminUserId(user.getId());
    session.setCreatedAt(now);
    session.setExpiresAt(expiresAt);
    adminSessionMapper.insert(session);
    return new LoginResponse(token, expiresAt, new AdminUserView(user.getId(), user.getUsername(), user.getDisplayName()));
  }

  /**
   * 撤销当前后台会话。
   */
  @Transactional
  public void logout(String token) {
    adminSessionMapper.deleteById(token);
  }

  /**
   * 根据 Bearer token 读取当前后台用户。
   */
  public AdminUserView requireUser(String authorizationHeader) {
    String token = requireToken(authorizationHeader);
    AdminUserView user = adminSessionMapper.findUserByToken(token);
    if (user == null) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "LOGIN_REQUIRED", "请先登录平台后台");
    }
    return user;
  }

  public String requireToken(String authorizationHeader) {
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "LOGIN_REQUIRED", "请先登录平台后台");
    }
    return authorizationHeader.substring("Bearer ".length()).trim();
  }
}
