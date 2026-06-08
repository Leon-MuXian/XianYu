package com.serenmeet.tenant.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 平台后台租户列表行。
 */
public record TenantListItem(
  Long id,
  String name,
  String city,
  LocalDate trialStartAt,
  LocalDate trialEndAt,
  String status,
  String statusText,
  String supportWechatId,
  Integer issuedCards,
  Integer deductions,
  Integer warningCount,
  BigDecimal monthlySalesYuan
) {
}
