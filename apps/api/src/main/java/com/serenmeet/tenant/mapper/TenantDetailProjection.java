package com.serenmeet.tenant.mapper;

import com.serenmeet.report.dto.BusinessSnapshotView;
import com.serenmeet.store.dto.StoreView;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * 租户详情 SQL 投影。
 */
@Getter
@Setter
public class TenantDetailProjection {

  private Long id;
  private String name;
  private String city;
  private String status;
  private LocalDate trialStartAt;
  private LocalDate trialEndAt;
  private String frozenReason;
  private String supportWechatId;
  private StoreView store;
  private BusinessSnapshotView snapshot;
}
