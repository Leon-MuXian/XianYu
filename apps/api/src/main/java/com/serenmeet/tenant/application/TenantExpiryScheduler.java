package com.serenmeet.tenant.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 租户试用到期自动冻结任务。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "seren-meet.tenant-expiry-freeze", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TenantExpiryScheduler {

  private final TenantAdminApplicationService tenantService;

  public TenantExpiryScheduler(TenantAdminApplicationService tenantService) {
    this.tenantService = tenantService;
  }

  @Scheduled(cron = "${seren-meet.tenant-expiry-freeze.cron:0 10 0 * * *}", zone = "${seren-meet.tenant-expiry-freeze.zone:Asia/Shanghai}")
  public void freezeExpiredTenants() {
    int frozenCount = tenantService.freezeExpiredTenants();
    if (frozenCount > 0) {
      log.info("Frozen {} expired tenant(s)", frozenCount);
    }
  }
}
