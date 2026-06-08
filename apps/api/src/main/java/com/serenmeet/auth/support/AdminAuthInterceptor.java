package com.serenmeet.auth.support;

import com.serenmeet.auth.application.AdminAuthApplicationService;
import com.serenmeet.auth.dto.AdminUserView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 统一校验后台接口 Bearer token。
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

  private final AdminAuthApplicationService authService;

  public AdminAuthInterceptor(AdminAuthApplicationService authService) {
    this.authService = authService;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String authorization = request.getHeader("Authorization");
    AdminUserView user = authService.requireUser(authorization);
    request.setAttribute(CurrentAdminUser.REQUEST_ATTRIBUTE, user);
    request.setAttribute(CurrentAdminUser.TOKEN_ATTRIBUTE, authService.requireToken(authorization));
    return true;
  }
}
