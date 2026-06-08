package com.serenmeet.tenant.application;

import com.serenmeet.audit.application.AuditApplicationService;
import com.serenmeet.tenant.domain.TenantEntity;
import com.serenmeet.auth.dto.AdminUserView;
import com.serenmeet.tenant.dto.ExtendTenantRequest;
import com.serenmeet.tenant.dto.FreezeTenantRequest;
import com.serenmeet.tenant.dto.TenantDetailResponse;
import com.serenmeet.tenant.dto.TenantListItem;
import com.serenmeet.tenant.dto.TenantListResponse;
import com.serenmeet.tenant.mapper.TenantDetailProjection;
import com.serenmeet.tenant.mapper.TenantMapper;
import com.serenmeet.tenant.mapper.TenantListRow;
import com.serenmeet.audit.domain.AuditAction;
import com.serenmeet.tenant.support.TenantStatus;
import com.serenmeet.common.ApiException;
import com.serenmeet.common.PageResponse;
import com.serenmeet.common.PageQuery;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 平台后台租户查询和状态操作服务。
 */
@Service
public class TenantAdminApplicationService {

  private static final String AUTO_EXPIRE_ACTOR = "tenant_expiry_scheduler";
  private static final String AUTO_EXPIRE_REASON = "试用或延长期限已过期，系统自动冻结租户";

  private final TenantMapper tenantMapper;
  private final AuditApplicationService auditService;
  private final Clock clock;

  public TenantAdminApplicationService(TenantMapper tenantMapper, AuditApplicationService auditService, Clock clock) {
    this.tenantMapper = tenantMapper;
    this.auditService = auditService;
    this.clock = clock;
  }

  /**
   * 查询租户列表，支持搜索、状态、到期筛选和分页。
   */
  public TenantListResponse listTenants(String keyword, String status, String expiry, int page, int pageSize) {
    PageQuery pageQuery = PageQuery.normalize(page, pageSize);
    long total = tenantMapper.countTenantList(keyword, status, expiry);
    return new TenantListResponse(
      tenantMapper.selectStats(),
      new PageResponse<TenantListItem>(
        tenantMapper.selectTenantList(keyword, status, expiry, pageQuery.pageSize(), pageQuery.offset()).stream()
          .map(this::toListItem)
          .toList(),
        total,
        pageQuery.page(),
        pageQuery.pageSize()
      )
    );
  }

  /**
   * 查询租户详情。
   */
  public TenantDetailResponse getTenant(Long tenantId) {
    TenantDetailProjection detail = tenantMapper.selectTenantDetail(tenantId);
    if (detail == null) {
      throw new ApiException(HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "租户不存在");
    }
    return toDetailResponse(detail);
  }

  /**
   * 自动冻结已过期且尚未冻结的租户。
   */
  @Transactional
  public int freezeExpiredTenants() {
    int frozenCount = 0;
    LocalDateTime now = LocalDateTime.now(clock);
    for (TenantEntity tenant : tenantMapper.selectExpiredActiveTenants()) {
      String oldStatus = tenant.getStatus();
      int affectedRows = tenantMapper.freezeExpiredTenant(tenant.getId(), AUTO_EXPIRE_REASON, now);
      if (affectedRows == 0) {
        continue;
      }
      auditService.recordSystem(
        tenant.getId(),
        AUTO_EXPIRE_ACTOR,
        AuditAction.FREEZE_TENANT.code(),
        tenant.getName(),
        oldStatus,
        TenantStatus.FROZEN.code(),
        AUTO_EXPIRE_REASON
      );
      frozenCount += 1;
    }
    return frozenCount;
  }

  /**
   * 冻结租户并写入审计。
   */
  @Transactional
  public TenantDetailResponse freezeTenant(Long tenantId, FreezeTenantRequest request, AdminUserView actor) {
    TenantEntity tenant = requireTenant(tenantId);
    if (TenantStatus.FROZEN.code().equals(tenant.getStatus())) {
      throw new ApiException(HttpStatus.CONFLICT, "TENANT_ALREADY_FROZEN", "租户已经处于冻结状态");
    }
    String oldStatus = tenant.getStatus();
    tenant.setStatus(TenantStatus.FROZEN.code());
    tenant.setFrozenReason(request.reason());
    tenant.setUpdatedAt(LocalDateTime.now(clock));
    tenantMapper.updateById(tenant);
    auditService.record(tenantId, actor.username(), AuditAction.FREEZE_TENANT.code(), tenant.getName(), oldStatus, TenantStatus.FROZEN.code(), request.reason());
    return getTenant(tenantId);
  }

  /**
   * 调整期限并解冻。
   */
  @Transactional
  public TenantDetailResponse extendTrial(Long tenantId, ExtendTenantRequest request, AdminUserView actor) {
    if (request.newTrialEndAt().isBefore(LocalDate.now(clock))) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TRIAL_END", "新到期日不能早于今天");
    }
    TenantEntity tenant = requireTenant(tenantId);
    if (!TenantStatus.FROZEN.code().equals(tenant.getStatus())) {
      throw new ApiException(HttpStatus.CONFLICT, "TENANT_NOT_FROZEN", "租户未冻结，不能延期并解冻");
    }
    String oldValue = tenant.getTrialEndAt().toString();
    tenant.setStatus(TenantStatus.EXTENDED.code());
    tenant.setTrialEndAt(request.newTrialEndAt());
    tenant.setFrozenReason(null);
    tenant.setUpdatedAt(LocalDateTime.now(clock));
    tenantMapper.updateById(tenant);
    String reason = request.internalNote() == null || request.internalNote().isBlank()
      ? request.reason()
      : request.reason() + "；" + request.internalNote();
    auditService.record(tenantId, actor.username(), AuditAction.EXTEND_TRIAL.code(), tenant.getName(), oldValue, request.newTrialEndAt().toString(), reason);
    return getTenant(tenantId);
  }

  private TenantEntity requireTenant(Long tenantId) {
    TenantEntity tenant = tenantMapper.selectById(tenantId);
    if (tenant == null) {
      throw new ApiException(HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "租户不存在");
    }
    return tenant;
  }

  private TenantListItem toListItem(TenantListRow row) {
    return new TenantListItem(
      row.id(),
      row.name(),
      row.city(),
      row.trialStartAt(),
      row.trialEndAt(),
      row.status(),
      TenantStatus.textOf(row.status()),
      row.supportWechatId(),
      row.issuedCards(),
      row.deductions(),
      row.warningCount(),
      row.monthlySalesYuan()
    );
  }

  private TenantDetailResponse toDetailResponse(TenantDetailProjection projection) {
    return new TenantDetailResponse(
      projection.getId(),
      projection.getName(),
      projection.getCity(),
      projection.getStatus(),
      TenantStatus.textOf(projection.getStatus()),
      projection.getTrialStartAt(),
      projection.getTrialEndAt(),
      projection.getFrozenReason(),
      projection.getSupportWechatId(),
      projection.getStore(),
      projection.getSnapshot()
    );
  }
}
