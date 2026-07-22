package com.serenmeet.auth.support;

import com.serenmeet.common.ApiException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "test"})
public class LocalWechatCodeSessionClient implements WechatCodeSessionClient {

  @Override
  public String exchange(String application, String code) {
    if (code == null || code.isBlank() || code.length() > 120) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "WECHAT_LOGIN_FAILED", "微信登录凭证无效");
    }
    return "local-" + application + "-" + code;
  }
}
