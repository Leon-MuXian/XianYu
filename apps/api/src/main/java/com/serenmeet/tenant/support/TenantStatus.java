package com.serenmeet.tenant.support;

/**
 * 租户生命周期状态。
 */
public enum TenantStatus {
  TRIALING("试用中"),
  EXPIRING("即将到期"),
  FROZEN("已冻结"),
  EXTENDED("已延长");

  private final String text;

  TenantStatus(String text) {
    this.text = text;
  }

  public String code() {
    return name();
  }

  public String text() {
    return text;
  }

  public static String textOf(String code) {
    if (code == null) {
      return null;
    }
    for (TenantStatus status : values()) {
      if (status.code().equals(code)) {
        return status.text();
      }
    }
    return code;
  }
}
