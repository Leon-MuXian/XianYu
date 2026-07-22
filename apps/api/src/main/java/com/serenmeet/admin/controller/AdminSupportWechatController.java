package com.serenmeet.admin.controller;

import com.serenmeet.admin.application.SupportWechatApplicationService;
import com.serenmeet.admin.dto.SupportWechatResponse;
import com.serenmeet.admin.dto.UpdateSupportWechatRequest;
import com.serenmeet.auth.dto.AdminUserView;
import com.serenmeet.auth.support.SessionPrincipal;
import com.serenmeet.common.ApiResponse;
import com.serenmeet.common.IdempotencyWorkflow;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台客服微信配置接口。
 */
@RestController
@RequestMapping("/admin/support-wechat")
public class AdminSupportWechatController {

  private final SupportWechatApplicationService supportWechatService;
  private final IdempotencyWorkflow idempotency;

  public AdminSupportWechatController(SupportWechatApplicationService supportWechatService, IdempotencyWorkflow idempotency) {
    this.supportWechatService = supportWechatService;
    this.idempotency = idempotency;
  }

  @GetMapping
  public ApiResponse<SupportWechatResponse> getSupportWechat() {
    return ApiResponse.ok(supportWechatService.getDefaultSupportWechat());
  }

  @PutMapping
  public ApiResponse<Object> updateSupportWechat(
    AdminUserView actor,
    @RequestHeader("Idempotency-Key") String idempotencyKey,
    @Valid @RequestBody UpdateSupportWechatRequest request
  ) {
    return ApiResponse.ok(idempotency.execute(
      new SessionPrincipal("admin", actor.id().toString(), null, null, true, false),
      "admin.update-support-wechat",
      idempotencyKey,
      request,
      () -> supportWechatService.updateDefaultSupportWechat(request, actor)
    ));
  }
}
