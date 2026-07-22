package com.serenmeet.common;

import com.serenmeet.auth.support.SessionPrincipal;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyWorkflow {

    private final IdempotencyStore store;

    public IdempotencyWorkflow(IdempotencyStore store) {
        this.store = store;
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public Object execute(
            SessionPrincipal principal, String operation, String key, Object request, Supplier<?> action) {
        IdempotencyStore.Claim claim = store.claim(principal, operation, key, request);
        if (claim.replayed()) {
            return claim.response();
        }
        try {
            Object response = action.get();
            store.complete(principal, operation, key, response);
            return response;
        } catch (RuntimeException exception) {
            // 业务执行失败必须释放占位，否则修正后的请求会被阻断 24 小时。
            store.release(principal, operation, key);
            throw exception;
        }
    }
}
