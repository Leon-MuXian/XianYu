package com.serenmeet.tenant.mapper;

import lombok.Getter;
import lombok.Setter;

/**
 * 租户详情中的门店数据库投影。
 */
@Getter
@Setter
public class TenantStoreProjection {

    private String name;
    private String serviceScopes;
    private String address;
    private String contactPhone;
    private String businessHours;
}
