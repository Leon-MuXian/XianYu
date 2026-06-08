package com.serenmeet.admin.controller;

import com.serenmeet.admin.application.SupportWechatApplicationService;
import com.serenmeet.admin.dto.SupportWechatResponse;
import com.serenmeet.admin.dto.UpdateSupportWechatRequest;
import com.serenmeet.auth.dto.AdminUserView;
import com.serenmeet.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台客服微信配置接口。
 */
@RestController
@RequestMapping("/admin/support-wechat")
public class AdminSupportWechatController {

  private final SupportWechatApplicationService supportWechatService;

  public AdminSupportWechatController(SupportWechatApplicationService supportWechatService) {
    this.supportWechatService = supportWechatService;
  }

  @GetMapping
  public ApiResponse<SupportWechatResponse> getSupportWechat() {
    return ApiResponse.ok(supportWechatService.getDefaultSupportWechat());
  }

  @PutMapping
  public ApiResponse<SupportWechatResponse> updateSupportWechat(
    AdminUserView actor,
    @Valid @RequestBody UpdateSupportWechatRequest request
  ) {
    return ApiResponse.ok(supportWechatService.updateDefaultSupportWechat(request, actor));
  }
}
