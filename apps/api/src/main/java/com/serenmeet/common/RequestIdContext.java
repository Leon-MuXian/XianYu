package com.serenmeet.common;

import java.util.UUID;

/**
 * 请求级 requestId 上下文。
 */
public final class RequestIdContext {

  public static final String HEADER_NAME = "X-Request-Id";
  private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

  private RequestIdContext() {
  }

  public static String get() {
    String requestId = CURRENT.get();
    if (requestId == null || requestId.isBlank()) {
      requestId = newRequestId();
      CURRENT.set(requestId);
    }
    return requestId;
  }

  public static void set(String requestId) {
    CURRENT.set(normalize(requestId));
  }

  public static void clear() {
    CURRENT.remove();
  }

  private static String normalize(String requestId) {
    if (requestId == null || requestId.isBlank()) {
      return newRequestId();
    }
    return requestId.trim();
  }

  private static String newRequestId() {
    return UUID.randomUUID().toString();
  }
}
