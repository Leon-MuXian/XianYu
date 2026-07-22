package com.serenmeet.auth.dto;

import java.time.OffsetDateTime;

public record BusinessLoginResponse(String token, OffsetDateTime expiresAt, SessionView session) {
}
