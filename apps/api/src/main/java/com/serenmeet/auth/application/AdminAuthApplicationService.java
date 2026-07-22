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
import com.serenmeet.auth.support.TokenHasher;
import com.serenmeet.common.ApiException;
import java.time.Clock;
import java.time.OffsetDateTime;
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
  private final TokenHasher tokenHasher;
  private final Clock clock;

  public AdminAuthApplicationService(
    AdminUserMapper adminUserMapper,
    AdminSessionMapper adminSessionMapper,
    PasswordHasher passwordHasher,
    AdminLoginRateLimiter loginRateLimiter,
    TokenHasher tokenHasher,
    Clock clock
  ) {
    this.adminUserMapper = adminUserMapper;
    this.adminSessionMapper = adminSessionMapper;
    this.passwordHasher = passwordHasher;
    this.loginRateLimiter = loginRateLimiter;
    this.tokenHasher = tokenHasher;
    this.clock = clock;
  }

  /**
   * 校验后台账号密码并创建数据库会话。
   */
  @Transactional
  public LoginResponse login(LoginRequest request) {
    loginRateLimiter.checkAllowed(request.username());
    AdminUserEntity user = adminUserMapper.selectOne(
      new LambdaQueryWrapper<AdminUserEntity>().eq(AdminUserEntity::getLoginName, request.username())
    );
    if (user == null || !"active".equals(user.getStatus()) || !passwordHasher.matches(request.password(), user.getPasswordHash())) {
      loginRateLimiter.recordFailure(request.username());
      throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "账号或密码错误");
    }
    loginRateLimiter.recordSuccess(request.username());

    String token = tokenHasher.generateToken();
    OffsetDateTime now = OffsetDateTime.now(clock);
    OffsetDateTime expiresAt = now.plusHours(12);
    AdminSessionEntity session = new AdminSessionEntity();
    session.setTokenHash(tokenHasher.hash(token));
    session.setActorType("admin");
    session.setSubjectId(user.getId().toString());
    session.setCreatedAt(now);
    session.setExpiresAt(expiresAt);
    adminSessionMapper.insert(session);
    return new LoginResponse(token, expiresAt, new AdminUserView(user.getId(), user.getLoginName(), user.getDisplayName()));
  }

  /**
   * 撤销当前后台会话。
   */
  @Transactional
  public void logout(String token) {
    adminSessionMapper.deleteById(tokenHasher.hash(token));
  }

  /**
   * 根据 Bearer token 读取当前后台用户。
   */
  public AdminUserView requireUser(String authorizationHeader) {
    String token = requireToken(authorizationHeader);
    AdminUserView user = adminSessionMapper.findAdminUserByTokenHash(tokenHasher.hash(token));
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
