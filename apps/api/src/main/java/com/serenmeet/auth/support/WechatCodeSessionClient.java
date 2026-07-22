package com.serenmeet.auth.support;

public interface WechatCodeSessionClient {
  String exchange(String application, String code);
}
