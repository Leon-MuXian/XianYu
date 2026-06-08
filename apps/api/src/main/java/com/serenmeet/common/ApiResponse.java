package com.serenmeet.common;

/**
 * 四端 API 的统一响应载体，保留 requestId 便于后台审计和问题追踪。
 */
public record ApiResponse<T>(String requestId, boolean success, T data, ApiError error) {

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(RequestIdContext.get(), true, data, null);
  }

  public static <T> ApiResponse<T> fail(String code, String message) {
    return new ApiResponse<>(RequestIdContext.get(), false, null, new ApiError(code, message));
  }
}
