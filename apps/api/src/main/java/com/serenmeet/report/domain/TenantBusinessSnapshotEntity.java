package com.serenmeet.report.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 租户试点业务摘要实体。
 */
@Getter
@Setter
@TableName("tenant_business_snapshot")
public class TenantBusinessSnapshotEntity {

  /** 对应租户 ID。 */
  @TableId
  private Long tenantId;

  /** 已发放会员卡数量。 */
  private Integer issuedCards;

  /** 到店确认数量。 */
  private Integer checkins;

  /** 核销数量。 */
  private Integer deductions;

  /** 未处理预警总数。 */
  private Integer warningCount;

  /** 低余额预警数量。 */
  private Integer lowBalanceCount;

  /** 即将到期预警数量。 */
  private Integer expiringCount;

  /** 本月线下售卡金额，单位元。 */
  private BigDecimal monthlySalesYuan;

  /** 最近一次业务活跃时间。 */
  private LocalDateTime lastActivityAt;
}
