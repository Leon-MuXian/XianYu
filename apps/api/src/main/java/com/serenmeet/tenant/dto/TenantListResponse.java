package com.serenmeet.tenant.dto;

import com.serenmeet.common.PageResponse;

/**
 * 租户列表页面响应。
 */
public record TenantListResponse(TenantStatsResponse stats, PageResponse<TenantListItem> page) {
}
