package com.serenmeet.auth.dto;

import java.time.OffsetDateTime;

/**
 * 平台后台登录成功响应。
 */
public record LoginResponse(String token, OffsetDateTime expiresAt, AdminUserView user) {
}
