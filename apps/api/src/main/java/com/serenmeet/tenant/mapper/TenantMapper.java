package com.serenmeet.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.serenmeet.tenant.domain.TenantEntity;
import com.serenmeet.tenant.dto.TenantStatsResponse;
import java.util.List;
import java.time.OffsetDateTime;
import org.apache.ibatis.annotations.Param;

/**
 * 租户 Mapper。
 */
@InterceptorIgnore(tenantLine = "true")
public interface TenantMapper extends BaseMapper<TenantEntity> {

  List<TenantListRow> selectTenantList(
    @Param("keyword") String keyword,
    @Param("status") String status,
    @Param("expiry") String expiry,
    @Param("pageSize") int pageSize,
    @Param("offset") int offset
  );

  long countTenantList(@Param("keyword") String keyword, @Param("status") String status, @Param("expiry") String expiry);

  TenantStatsResponse selectStats();

  TenantDetailProjection selectTenantDetail(@Param("tenantId") Long tenantId);

  List<TenantEntity> selectExpiredActiveTenants();

  int freezeExpiredTenant(
    @Param("tenantId") Long tenantId,
    @Param("reason") String reason,
    @Param("updatedAt") OffsetDateTime updatedAt
  );
}
