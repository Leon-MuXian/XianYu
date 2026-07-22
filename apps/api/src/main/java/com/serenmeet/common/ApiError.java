package com.serenmeet.common;

import java.util.Map;

/**
 * API 错误信息，面向前端展示页面级或表单级错误。
 */
public record ApiError(String code, String message, Map<String, String> fieldErrors) {

  public ApiError(String code, String message) {
    this(code, message, null);
  }
}
