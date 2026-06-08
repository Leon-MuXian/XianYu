package com.serenmeet.audit.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 平台操作审计实体。
 */
@Getter
@Setter
@TableName("audit_log")
public class AuditLogEntity {

  /** 审计记录主键。 */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 相关租户 ID；平台全局配置为空。 */
  private Long tenantId;

  /** 操作人类型：admin、system 等。 */
  private String actorType;

  /** 操作人账号或系统来源。 */
  private String actorName;

  /** 操作动作，例如 FREEZE_TENANT、UPDATE_CONFIG。 */
  private String action;

  /** 操作对象名称，例如租户名或配置项名。 */
  private String targetName;

  /** 变更前值。 */
  private String oldValue;

  /** 变更后值。 */
  private String newValue;

  /** 操作原因，冻结、延期和配置修改必须填写。 */
  private String reason;

  /** 操作发生时间。 */
  private LocalDateTime createdAt;
}
