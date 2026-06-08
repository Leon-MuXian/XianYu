package com.serenmeet.audit.domain;

/**
 * 审计操作者类型。
 */
public enum AuditActorType {
  ADMIN,
  SYSTEM;

  public String code() {
    return name().toLowerCase();
  }
}
