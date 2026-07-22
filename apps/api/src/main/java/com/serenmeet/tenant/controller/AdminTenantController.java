package com.serenmeet.tenant.controller;

import com.serenmeet.auth.dto.AdminUserView;
import com.serenmeet.auth.support.SessionPrincipal;
import com.serenmeet.common.IdempotencyWorkflow;
import com.serenmeet.tenant.application.TenantAdminApplicationService;
import com.serenmeet.tenant.dto.ExtendTenantRequest;
import com.serenmeet.tenant.dto.FreezeTenantRequest;
import com.serenmeet.tenant.dto.TenantDetailResponse;
import com.serenmeet.tenant.dto.TenantListResponse;
import com.serenmeet.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台后台租户管理接口。
 */
@RestController
@RequestMapping("/admin/tenants")
public class AdminTenantController {

  private final TenantAdminApplicationService tenantService;
  private final IdempotencyWorkflow idempotency;

  public AdminTenantController(TenantAdminApplicationService tenantService, IdempotencyWorkflow idempotency) {
    this.tenantService = tenantService;
    this.idempotency = idempotency;
  }

  /**
   * 租户列表。
   */
  @GetMapping
  public ApiResponse<TenantListResponse> listTenants(
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) String status,
    @RequestParam(required = false) String expiry,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int pageSize
  ) {
    return ApiResponse.ok(tenantService.listTenants(keyword, status, expiry, page, pageSize));
  }

  /**
   * 租户详情。
   */
  @GetMapping("/{tenantId}")
  public ApiResponse<TenantDetailResponse> getTenant(
    @PathVariable Long tenantId
  ) {
    return ApiResponse.ok(tenantService.getTenant(tenantId));
  }

  /**
   * 冻结租户。
   */
  @PostMapping("/{tenantId}/freeze")
  public ApiResponse<Object> freezeTenant(
    AdminUserView actor,
    @PathVariable Long tenantId,
    @RequestHeader("Idempotency-Key") String idempotencyKey,
    @Valid @RequestBody FreezeTenantRequest request
  ) {
    return ApiResponse.ok(idempotency.execute(
      adminPrincipal(actor),
      "admin.freeze-tenant." + tenantId,
      idempotencyKey,
      request,
      () -> tenantService.freezeTenant(tenantId, request, actor)
    ));
  }

  /**
   * 调整期限并解冻。
   */
  @PostMapping("/{tenantId}/extend-trial")
  public ApiResponse<Object> extendTrial(
    AdminUserView actor,
    @PathVariable Long tenantId,
    @RequestHeader("Idempotency-Key") String idempotencyKey,
    @Valid @RequestBody ExtendTenantRequest request
  ) {
    return ApiResponse.ok(idempotency.execute(
      adminPrincipal(actor),
      "admin.extend-tenant." + tenantId,
      idempotencyKey,
      request,
      () -> tenantService.extendTrial(tenantId, request, actor)
    ));
  }

  private SessionPrincipal adminPrincipal(AdminUserView actor) {
    return new SessionPrincipal("admin", actor.id().toString(), null, null, true, false);
  }
}
