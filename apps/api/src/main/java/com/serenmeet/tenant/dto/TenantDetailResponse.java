package com.serenmeet.tenant.dto;

import com.serenmeet.report.dto.BusinessSnapshotView;
import com.serenmeet.store.dto.StoreView;
import java.time.LocalDate;

/**
 * 平台后台租户详情响应。
 */
public record TenantDetailResponse(
  Long id,
  String name,
  String city,
  String status,
  String statusText,
  LocalDate trialStartAt,
  LocalDate trialEndAt,
  String frozenReason,
  String supportWechatId,
  StoreView store,
  BusinessSnapshotView snapshot
) {
}
