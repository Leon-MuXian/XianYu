package com.serenmeet.tenant.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 租户列表 SQL 查询行。
 */
public record TenantListRow(
  Long id,
  String name,
  String city,
  LocalDate trialStartAt,
  LocalDate trialEndAt,
  String status,
  String supportWechatId,
  Integer issuedCards,
  Integer deductions,
  Integer warningCount,
  BigDecimal monthlySalesYuan
) {
}
