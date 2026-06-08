package com.serenmeet.auth.dto;

import java.time.LocalDateTime;

/**
 * 平台后台登录成功响应。
 */
public record LoginResponse(String token, LocalDateTime expiresAt, AdminUserView user) {
}
