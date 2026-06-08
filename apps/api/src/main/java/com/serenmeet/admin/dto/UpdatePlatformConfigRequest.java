package com.serenmeet.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 修改平台配置请求。
 */
public record UpdatePlatformConfigRequest(
  @NotBlank(message = "请填写新值") String newValue,
  @NotBlank(message = "请填写操作原因") String reason
) {
}
