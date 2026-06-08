package com.serenmeet.auth.application;

import static org.mockito.Mockito.verify;

import com.serenmeet.auth.mapper.AdminSessionMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminSessionCleanupSchedulerTest {

  @Mock
  private AdminSessionMapper adminSessionMapper;

  @Test
  void cleanupDeletesExpiredSessionsUsingBusinessClock() {
    Clock clock = Clock.fixed(Instant.parse("2026-06-07T10:00:00Z"), ZoneId.of("Asia/Shanghai"));

    new AdminSessionCleanupScheduler(adminSessionMapper, clock).cleanupExpiredSessions();

    verify(adminSessionMapper).deleteExpiredSessions(LocalDateTime.of(2026, 6, 7, 18, 0));
  }
}
