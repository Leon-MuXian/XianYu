package com.serenmeet.store.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 门店资料实体。
 */
@Getter
@Setter
@TableName("store")
public class StoreEntity {

  /** 门店主键。 */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 所属租户 ID。 */
  private Long tenantId;

  /** 门店名称。 */
  private String name;

  /** 店长自填经营项目，逗号分隔展示。 */
  private String businessCategories;

  /** 店长自填服务标签，逗号分隔展示。 */
  private String serviceTags;

  /** 门店地址。 */
  private String address;

  /** 门店联系电话。 */
  private String contactPhone;

  /** 营业时间文本摘要。 */
  private String businessHours;
}
