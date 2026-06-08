package com.serenmeet.tenant.dto;

/**
 * 租户列表顶部状态指标。
 */
public record TenantStatsResponse(long total, long trialing, long expiring, long frozen, long extended) {
}
