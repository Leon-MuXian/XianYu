package com.serenmeet.admin.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 平台客服微信文本配置实体，不承载二维码、图片或附件。
 */
@Getter
@Setter
@TableName("support_wechat")
public class SupportWechatEntity {

  /** 客服微信配置主键。 */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 租户 ID；为空表示默认客服微信。 */
  private Long tenantId;

  /** 展示给店长的客服微信文本 ID。 */
  private String wechatId;

  /** 冻结页客服说明文案。 */
  private String displayText;

  /** 客服微信展示范围说明。 */
  private String displayScope;

  /** 是否启用展示。 */
  private Boolean enabled;

  /** 最近更新时间。 */
  private LocalDateTime updatedAt;
}
