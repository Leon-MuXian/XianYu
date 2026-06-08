package com.serenmeet.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 保存客服微信文本配置请求。
 */
public record UpdateSupportWechatRequest(
  @NotBlank(message = "请填写客服微信 ID") String wechatId,
  @NotBlank(message = "请填写展示文案") String displayText,
  @NotBlank(message = "请填写展示范围") String displayScope,
  Boolean enabled,
  @NotBlank(message = "请填写操作原因") String reason
) {
}
