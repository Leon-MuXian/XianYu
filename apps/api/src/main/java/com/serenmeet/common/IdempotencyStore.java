package com.serenmeet.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serenmeet.auth.support.SessionPrincipal;
import com.serenmeet.auth.support.TokenHasher;
import com.serenmeet.common.mapper.IdempotencyRequestMapper;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 保存关键写请求的幂等占位与响应，并支持安全重放。 */
@Service
public class IdempotencyStore {

    private static final int CLAIM_VALID_HOURS = 24;
    private static final int MAXIMUM_KEY_LENGTH = 120;

    private final IdempotencyRequestMapper requestMapper;
    private final ObjectMapper objectMapper;
    private final TokenHasher tokenHasher;
    private final Clock clock;

    public IdempotencyStore(
            IdempotencyRequestMapper requestMapper,
            ObjectMapper objectMapper,
            TokenHasher tokenHasher,
            Clock clock) {
        this.requestMapper = requestMapper;
        this.objectMapper = objectMapper;
        this.tokenHasher = tokenHasher;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Claim claim(SessionPrincipal principal, String operation, String key, Object request) {
        validateKey(key);
        String requestHash = hashRequest(request);
        int inserted = requestMapper.insertClaim(
                principal.actorType(),
                principal.subjectId(),
                operation,
                key,
                requestHash,
                OffsetDateTime.now(clock).plusHours(CLAIM_VALID_HOURS));
        if (inserted == 1) {
            return new Claim(false, null);
        }
        Map<String, Object> stored = requestMapper.selectClaim(
                principal.actorType(), principal.subjectId(), operation, key);
        if (stored == null || !requestHash.equals(stored.get("requestHash"))) {
            throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", "同一幂等键不能用于不同请求");
        }
        Object responseBody = stored.get("responseBody");
        if (responseBody == null) {
            throw new ApiException(HttpStatus.CONFLICT, "REQUEST_DUPLICATED", "相同请求正在处理中");
        }
        return new Claim(true, parseResponse(responseBody.toString()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(SessionPrincipal principal, String operation, String key, Object response) {
        try {
            requestMapper.completeClaim(
                    principal.actorType(),
                    principal.subjectId(),
                    operation,
                    key,
                    objectMapper.writeValueAsString(response));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize idempotency response", exception);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(SessionPrincipal principal, String operation, String key) {
        requestMapper.releaseClaim(principal.actorType(), principal.subjectId(), operation, key);
    }

    private JsonNode parseResponse(String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored idempotency response is invalid", exception);
        }
    }

    private String hashRequest(Object request) {
        try {
            return tokenHasher.hash(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "请求体无法处理");
        }
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > MAXIMUM_KEY_LENGTH) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "IDEMPOTENCY_KEY_REQUIRED",
                    "关键操作必须提供有效的 Idempotency-Key");
        }
    }

    public record Claim(boolean replayed, JsonNode response) {
    }
}
