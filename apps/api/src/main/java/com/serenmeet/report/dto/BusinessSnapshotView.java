package com.serenmeet.report.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 租户业务摘要，辅助平台判断试点活跃度。
 */
public record BusinessSnapshotView(
  Integer issuedCards,
  Integer checkins,
  Integer deductions,
  Integer warningCount,
  Integer lowBalanceCount,
  Integer expiringCount,
  BigDecimal monthlySalesYuan,
  OffsetDateTime lastActivityAt
) {
}
