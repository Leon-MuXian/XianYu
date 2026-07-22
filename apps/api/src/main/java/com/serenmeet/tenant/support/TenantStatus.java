package com.serenmeet.tenant.support;

/**
 * 租户生命周期状态。
 */
public enum TenantStatus {
  TRIALING("trialing", "试用中"),
  EXPIRING("expiring", "即将到期"),
  FROZEN("frozen", "已冻结"),
  EXTENDED("extended", "已延长");

  private final String code;
  private final String text;

  TenantStatus(String code, String text) {
    this.code = code;
    this.text = text;
  }

  public String code() {
    return code;
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
