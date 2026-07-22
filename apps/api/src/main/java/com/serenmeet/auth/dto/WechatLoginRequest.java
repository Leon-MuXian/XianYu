package com.serenmeet.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record WechatLoginRequest(@NotBlank(message = "微信登录凭证不能为空") String code) {
}
