package com.serenmeet.admin.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 平台运营配置实体，只影响后续新业务流程。
 */
@Getter
@Setter
@TableName("platform_config")
public class PlatformConfigEntity {

  /** 配置键，作为业务稳定标识。 */
  @TableId
  private String configKey;

  /** 配置项中文名称。 */
  private String name;

  /** 当前配置值，以文本保存并按类型校验。 */
  private String valueText;

  /** 值类型，例如 INTEGER。 */
  private String valueType;

  /** 配置单位，例如天、分钟。 */
  private String unit;

  /** 数值型配置的最小允许值。 */
  private Integer minValue;

  /** 数值型配置的最大允许值。 */
  private Integer maxValue;

  /** 配置影响范围说明。 */
  private String impactScope;

  /** 是否允许后台修改。 */
  private Boolean editable;

  /** 配置是否启用。 */
  private Boolean enabled;

  /** 最近更新时间。 */
  private LocalDateTime updatedAt;
}
