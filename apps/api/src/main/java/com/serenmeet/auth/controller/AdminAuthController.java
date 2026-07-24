package com.serenmeet.auth.controller;

import com.serenmeet.auth.application.AdminAuthApplicationService;
import com.serenmeet.auth.dto.AdminSessionToken;
import com.serenmeet.auth.dto.AdminUserView;
import com.serenmeet.auth.dto.LoginRequest;
import com.serenmeet.auth.dto.LoginResponse;
import com.serenmeet.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台后台认证接口。
 */
@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {

    private final AdminAuthApplicationService authService;

    public AdminAuthController(AdminAuthApplicationService authService) {
        this.authService = authService;
    }

    /**
     * 后台账号密码登录。
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    /**
     * 读取当前已登录的平台管理员。
     */
    @GetMapping("/me")
    public ApiResponse<AdminUserView> me(AdminUserView user) {
        return ApiResponse.ok(user);
    }

    /**
     * 退出后台登录。
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(AdminSessionToken token) {
        authService.logout(token.value());
        return ApiResponse.ok(null);
    }
}
