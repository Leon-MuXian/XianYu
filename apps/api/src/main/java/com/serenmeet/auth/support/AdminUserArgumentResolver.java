package com.serenmeet.auth.support;

import com.serenmeet.auth.dto.AdminSessionToken;
import com.serenmeet.auth.dto.AdminUserView;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 将已认证后台用户注入 Controller 方法参数。
 */
@Component
public class AdminUserArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return AdminUserView.class.equals(parameter.getParameterType())
      || AdminSessionToken.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
    MethodParameter parameter,
    ModelAndViewContainer mavContainer,
    NativeWebRequest webRequest,
    WebDataBinderFactory binderFactory
  ) {
    if (AdminSessionToken.class.equals(parameter.getParameterType())) {
      String token = CurrentAdminUser.requireToken(webRequest.getAttribute(CurrentAdminUser.TOKEN_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST));
      return new AdminSessionToken(token);
    }
    return CurrentAdminUser.require(webRequest.getAttribute(CurrentAdminUser.REQUEST_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST));
  }
}
