package com.serenmeet.admin.controller;

import com.serenmeet.admin.application.PlatformConfigApplicationService;
import com.serenmeet.admin.dto.PlatformConfigResponse;
import com.serenmeet.admin.dto.UpdatePlatformConfigRequest;
import com.serenmeet.auth.dto.AdminUserView;
import com.serenmeet.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台运营配置接口。
 */
@RestController
@RequestMapping("/admin/configs")
public class AdminPlatformConfigController {

  private final PlatformConfigApplicationService configService;

  public AdminPlatformConfigController(PlatformConfigApplicationService configService) {
    this.configService = configService;
  }

  @GetMapping
  public ApiResponse<List<PlatformConfigResponse>> listConfigs(
    @RequestParam(required = false) String keyword,
    @RequestParam(defaultValue = "false") Boolean editableOnly
  ) {
    return ApiResponse.ok(configService.listConfigs(keyword, editableOnly));
  }

  @PutMapping("/{configKey}")
  public ApiResponse<PlatformConfigResponse> updateConfig(
    AdminUserView actor,
    @PathVariable String configKey,
    @Valid @RequestBody UpdatePlatformConfigRequest request
  ) {
    return ApiResponse.ok(configService.updateConfig(configKey, request, actor));
  }
}
