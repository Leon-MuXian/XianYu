package com.serenmeet.audit.domain;

/**
 * 平台审计动作。
 */
public enum AuditAction {
  FREEZE_TENANT,
  EXTEND_TRIAL,
  UPDATE_SUPPORT_WECHAT,
  UPDATE_CONFIG;

  public String code() {
    return name();
  }
}
