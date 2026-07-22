package com.serenmeet.tenant.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * SaaS 租户实体，租户只由店长首次微信登录自动创建。
 */
@Getter
@Setter
@TableName("tenant")
public class TenantEntity {

  /** 租户主键。 */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 租户名称，通常取开通时的门店名称。 */
  private String name;

  /** 租户所在城市，用于后台筛选和运营判断。 */
  private String city;

  /** 租户状态：trialing、expiring、frozen、extended。 */
  private String status;

  /** 试用开始日期。 */
  private OffsetDateTime trialStartAt;

  /** 试用或人工延长后的到期日期。 */
  private OffsetDateTime trialEndAt;

  /** 冻结原因，未冻结时为空。 */
  @TableField("freeze_reason")
  private String freezeReason;

  /** 最近冻结时间，未冻结时为空。 */
  private OffsetDateTime frozenAt;

  /** 租户自动开通时间。 */
  private OffsetDateTime createdAt;

  /** 租户状态或期限最近更新时间。 */
  private OffsetDateTime updatedAt;
}
