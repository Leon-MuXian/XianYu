package com.serenmeet.store.dto;

/**
 * 租户详情中的门店资料。
 */
public record StoreView(
    String name,
    String serviceScopes,
    String address,
    String contactPhone,
    BusinessHoursView businessHours
) {
}
