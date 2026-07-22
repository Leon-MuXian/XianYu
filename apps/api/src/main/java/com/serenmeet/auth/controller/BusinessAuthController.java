package com.serenmeet.auth.controller;

import com.serenmeet.auth.application.BusinessAuthApplicationService;
import com.serenmeet.auth.dto.BusinessLoginResponse;
import com.serenmeet.auth.dto.SessionView;
import com.serenmeet.auth.dto.StaffLoginRequest;
import com.serenmeet.auth.dto.WechatLoginRequest;
import com.serenmeet.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class BusinessAuthController {

  private final BusinessAuthApplicationService authService;

  public BusinessAuthController(BusinessAuthApplicationService authService) {
    this.authService = authService;
  }

  @PostMapping("/owner/wechat-login")
  public ApiResponse<BusinessLoginResponse> ownerLogin(@Valid @RequestBody WechatLoginRequest request) {
    return ApiResponse.ok(authService.ownerWechatLogin(request.code()));
  }

  @PostMapping("/member/wechat-login")
  public ApiResponse<BusinessLoginResponse> memberLogin(@Valid @RequestBody WechatLoginRequest request) {
    return ApiResponse.ok(authService.memberWechatLogin(request.code()));
  }

  @PostMapping("/staff/login")
  public ApiResponse<BusinessLoginResponse> staffLogin(@Valid @RequestBody StaffLoginRequest request) {
    return ApiResponse.ok(authService.staffLogin(request));
  }

  @GetMapping("/session")
  public ApiResponse<SessionView> session(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
    return ApiResponse.ok(authService.session(authorization));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
    authService.logout(authorization);
    return ApiResponse.ok(null);
  }
}
