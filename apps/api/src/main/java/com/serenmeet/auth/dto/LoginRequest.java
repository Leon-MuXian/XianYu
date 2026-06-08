package com.serenmeet.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 平台后台账号密码登录请求。
 */
public record LoginRequest(
  @NotBlank(message = "请输入后台账号") String username,
  @NotBlank(message = "请输入登录密码") String password
) {
}
