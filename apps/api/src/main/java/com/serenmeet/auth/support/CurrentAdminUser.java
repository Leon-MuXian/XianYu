package com.serenmeet.auth.support;

import com.serenmeet.auth.dto.AdminUserView;
import com.serenmeet.common.ApiException;
import org.springframework.http.HttpStatus;

/**
 * 当前后台用户请求上下文。
 */
public final class CurrentAdminUser {

  public static final String REQUEST_ATTRIBUTE = CurrentAdminUser.class.getName() + ".user";
  public static final String TOKEN_ATTRIBUTE = CurrentAdminUser.class.getName() + ".token";

  private CurrentAdminUser() {
  }

  public static AdminUserView require(Object value) {
    if (value instanceof AdminUserView user) {
      return user;
    }
    throw new ApiException(HttpStatus.UNAUTHORIZED, "LOGIN_REQUIRED", "请先登录平台后台");
  }

  public static String requireToken(Object value) {
    if (value instanceof String token && !token.isBlank()) {
      return token;
    }
    throw new ApiException(HttpStatus.UNAUTHORIZED, "LOGIN_REQUIRED", "请先登录平台后台");
  }
}
