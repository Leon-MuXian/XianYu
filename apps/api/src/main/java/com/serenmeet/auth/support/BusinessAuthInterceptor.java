package com.serenmeet.auth.support;

import com.serenmeet.auth.application.BusinessAuthApplicationService;
import com.serenmeet.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class BusinessAuthInterceptor implements HandlerInterceptor {

  private final BusinessAuthApplicationService authService;

  public BusinessAuthInterceptor(BusinessAuthApplicationService authService) {
    this.authService = authService;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    TenantContext.clear();
    SessionPrincipal principal = authService.requirePrincipal(request.getHeader("Authorization"));
    String expectedRole = request.getRequestURI().split("/", 3)[1];
    if (!expectedRole.equals(principal.actorType())) {
      throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "当前账号无权访问此端");
    }
    enforceFrozenRoute(request, principal);
    if (principal.tenantId() != null) {
      TenantContext.set(principal.tenantId());
    }
    request.setAttribute(CurrentSession.REQUEST_ATTRIBUTE, principal);
    request.setAttribute(CurrentSession.TOKEN_ATTRIBUTE, authService.requireToken(request.getHeader("Authorization")));
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
    TenantContext.clear();
  }

  private void enforceFrozenRoute(HttpServletRequest request, SessionPrincipal principal) {
    if (!"frozen".equals(principal.tenantStatus())) {
      return;
    }
    String path = request.getRequestURI();
    boolean allowed = path.equals("/" + principal.actorType() + "/frozen")
      || ("member".equals(principal.actorType()) && (path.equals("/member/cards") || path.equals("/member/me")));
    if (!allowed) {
      throw new ApiException(HttpStatus.FORBIDDEN, "TENANT_FROZEN", "门店服务已冻结");
    }
  }
}
