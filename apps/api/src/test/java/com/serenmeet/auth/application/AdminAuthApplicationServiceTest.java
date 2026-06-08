package com.serenmeet.auth.application;

import static org.mockito.Mockito.verify;

import com.serenmeet.auth.mapper.AdminSessionMapper;
import com.serenmeet.auth.mapper.AdminUserMapper;
import com.serenmeet.auth.support.AdminLoginRateLimiter;
import com.serenmeet.auth.support.PasswordHasher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminAuthApplicationServiceTest {

  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-07T10:00:00Z"), ZoneId.of("Asia/Shanghai"));

  @Mock
  private AdminUserMapper adminUserMapper;

  @Mock
  private AdminSessionMapper adminSessionMapper;

  @Mock
  private PasswordHasher passwordHasher;

  @Mock
  private AdminLoginRateLimiter loginRateLimiter;

  private AdminAuthApplicationService service;

  @BeforeEach
  void setUp() {
    service = new AdminAuthApplicationService(adminUserMapper, adminSessionMapper, passwordHasher, loginRateLimiter, CLOCK);
  }

  @Test
  void logoutRevokesSessionToken() {
    service.logout("session-token");

    verify(adminSessionMapper).deleteById("session-token");
  }
}
