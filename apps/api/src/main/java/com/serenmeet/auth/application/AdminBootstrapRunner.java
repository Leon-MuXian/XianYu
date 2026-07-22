package com.serenmeet.auth.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.serenmeet.auth.domain.AdminUserEntity;
import com.serenmeet.auth.mapper.AdminUserMapper;
import com.serenmeet.auth.support.PasswordHasher;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 在管理员表为空时，根据显式环境变量一次性创建首个平台管理员。
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

  private final AdminUserMapper adminUserMapper;
  private final PasswordHasher passwordHasher;
  private final Clock clock;
  private final String username;
  private final String password;
  private final String displayName;

  public AdminBootstrapRunner(
    AdminUserMapper adminUserMapper,
    PasswordHasher passwordHasher,
    Clock clock,
    @Value("${seren-meet.bootstrap-admin.username:}") String username,
    @Value("${seren-meet.bootstrap-admin.password:}") String password,
    @Value("${seren-meet.bootstrap-admin.display-name:}") String displayName
  ) {
    this.adminUserMapper = adminUserMapper;
    this.passwordHasher = passwordHasher;
    this.clock = clock;
    this.username = username.trim();
    this.password = password;
    this.displayName = displayName.trim();
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    boolean anyConfigured = !username.isBlank() || !password.isBlank() || !displayName.isBlank();
    if (!anyConfigured || adminUserMapper.selectCount(new LambdaQueryWrapper<>()) > 0) {
      return;
    }
    if (username.isBlank() || password.isBlank() || displayName.isBlank()) {
      throw new IllegalStateException("Bootstrap admin username, password and display name must be configured together");
    }
    if (password.length() < 12) {
      throw new IllegalStateException("Bootstrap admin password must contain at least 12 characters");
    }
    OffsetDateTime now = OffsetDateTime.now(clock);
    AdminUserEntity user = new AdminUserEntity();
    user.setLoginName(username);
    user.setPasswordHash(passwordHasher.encode(password));
    user.setDisplayName(displayName);
    user.setStatus("active");
    user.setCreatedAt(now);
    user.setUpdatedAt(now);
    adminUserMapper.insert(user);
  }
}
