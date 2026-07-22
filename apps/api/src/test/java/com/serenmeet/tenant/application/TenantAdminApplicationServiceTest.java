package com.serenmeet.tenant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.serenmeet.tenant.domain.TenantEntity;
import com.serenmeet.auth.dto.AdminUserView;
import com.serenmeet.tenant.dto.ExtendTenantRequest;
import com.serenmeet.tenant.dto.TenantDetailResponse;
import com.serenmeet.tenant.mapper.TenantDetailProjection;
import com.serenmeet.tenant.mapper.TenantMapper;
import com.serenmeet.audit.application.AuditApplicationService;
import com.serenmeet.audit.domain.AuditAction;
import com.serenmeet.tenant.support.TenantStatus;
import com.serenmeet.common.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantAdminApplicationServiceTest {

  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-07T10:00:00Z"), ZoneId.of("Asia/Shanghai"));

  @Mock
  private TenantMapper tenantMapper;

  @Mock
  private AuditApplicationService auditService;

  private TenantAdminApplicationService service;

  @BeforeEach
  void setUp() {
    service = new TenantAdminApplicationService(tenantMapper, auditService, CLOCK);
  }

  @Test
  void extendTrialRejectsTenantThatIsNotFrozen() {
    TenantEntity tenant = tenant(1L, "和序预约中心", TenantStatus.TRIALING.code(), LocalDate.of(2026, 6, 20));
    when(tenantMapper.selectById(1L)).thenReturn(tenant);

    ExtendTenantRequest request = new ExtendTenantRequest(LocalDate.of(2026, 6, 30), "客服确认续期", null);

    assertThatThrownBy(() -> service.extendTrial(1L, request, actor()))
      .isInstanceOf(ApiException.class)
      .hasMessage("租户未冻结，不能延期并解冻");

    verify(tenantMapper, never()).updateById(any(TenantEntity.class));
    verify(auditService, never()).record(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void extendTrialUnfreezesFrozenTenant() {
    TenantEntity tenant = tenant(2L, "鲸喜训练", TenantStatus.FROZEN.code(), LocalDate.of(2026, 5, 28));
    tenant.setFreezeReason("试用到期");
    TenantDetailProjection detail = new TenantDetailProjection();
    detail.setId(2L);
    detail.setStatus(TenantStatus.EXTENDED.code());
    when(tenantMapper.selectById(2L)).thenReturn(tenant);
    when(tenantMapper.selectTenantDetail(2L)).thenReturn(detail);

    ExtendTenantRequest request = new ExtendTenantRequest(LocalDate.of(2026, 6, 30), "客服确认续期", "线下确认");
    TenantDetailResponse updated = service.extendTrial(2L, request, actor());

    assertThat(updated.status()).isEqualTo(TenantStatus.EXTENDED.code());
    assertThat(tenant.getStatus()).isEqualTo(TenantStatus.EXTENDED.code());
    assertThat(tenant.getFreezeReason()).isNull();
    assertThat(tenant.getTrialEndAt()).isEqualTo(
      LocalDate.of(2026, 6, 30).atTime(LocalTime.MAX).atZone(CLOCK.getZone()).toOffsetDateTime()
    );
    verify(tenantMapper).updateById(tenant);
    verify(auditService).record(
      eq(2L),
      eq("admin@serenmeet"),
      eq(AuditAction.EXTEND_TRIAL.code()),
      eq("鲸喜训练"),
      eq("2026-05-28"),
      eq("2026-06-30"),
      eq("客服确认续期；线下确认")
    );
  }

  @Test
  void freezeExpiredTenantsWritesAuditOnlyWhenConditionalUpdateSucceeds() {
    TenantEntity first = tenant(1L, "过期租户 A", TenantStatus.EXPIRING.code(), LocalDate.of(2026, 6, 1));
    TenantEntity second = tenant(2L, "过期租户 B", TenantStatus.EXTENDED.code(), LocalDate.of(2026, 6, 2));
    when(tenantMapper.selectExpiredActiveTenants()).thenReturn(List.of(first, second));
    when(tenantMapper.freezeExpiredTenant(eq(1L), any(), any())).thenReturn(1);
    when(tenantMapper.freezeExpiredTenant(eq(2L), any(), any())).thenReturn(0);

    int frozenCount = service.freezeExpiredTenants();

    assertThat(frozenCount).isEqualTo(1);
    verify(auditService).recordSystem(
      eq(1L),
      eq("tenant_expiry_scheduler"),
      eq(AuditAction.FREEZE_TENANT.code()),
      eq("过期租户 A"),
      eq(TenantStatus.EXPIRING.code()),
      eq(TenantStatus.FROZEN.code()),
      eq("试用或延长期限已过期，系统自动冻结租户")
    );
    verify(auditService, never()).recordSystem(eq(2L), any(), any(), any(), any(), any(), any());
  }

  private TenantEntity tenant(Long id, String name, String status, LocalDate trialEndAt) {
    TenantEntity tenant = new TenantEntity();
    tenant.setId(id);
    tenant.setName(name);
    tenant.setStatus(status);
    tenant.setTrialEndAt(trialEndAt.atStartOfDay(CLOCK.getZone()).toOffsetDateTime());
    return tenant;
  }

  private AdminUserView actor() {
    return new AdminUserView(1L, "admin@serenmeet", "平台管理员");
  }
}
