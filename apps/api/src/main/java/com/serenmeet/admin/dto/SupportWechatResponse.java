package com.serenmeet.admin.dto;

/**
 * 客服微信配置响应。
 */
public record SupportWechatResponse(
  Long id,
  String wechatId,
  String displayText,
  String displayScope,
  Boolean enabled
) {
}
