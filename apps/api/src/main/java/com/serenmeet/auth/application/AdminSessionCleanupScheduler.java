package com.serenmeet.auth.application;

import com.serenmeet.auth.mapper.AdminSessionMapper;
import java.time.Clock;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 清理过期后台登录会话。
 */
@Slf4j
@Component
public class AdminSessionCleanupScheduler {

  private final AdminSessionMapper adminSessionMapper;
  private final Clock clock;

  public AdminSessionCleanupScheduler(AdminSessionMapper adminSessionMapper, Clock clock) {
    this.adminSessionMapper = adminSessionMapper;
    this.clock = clock;
  }

  @Scheduled(cron = "${seren-meet.admin-auth.session-cleanup-cron:0 30 3 * * *}", zone = "${seren-meet.admin-auth.zone:Asia/Shanghai}")
  public void cleanupExpiredSessions() {
    int deleted = adminSessionMapper.deleteExpiredSessions(OffsetDateTime.now(clock));
    if (deleted > 0) {
      log.info("Cleaned expired authentication sessions: {}", deleted);
    }
  }
}
