package com.serenmeet.auth.support;

public record SessionPrincipal(
  String actorType,
  String subjectId,
  Long tenantId,
  String tenantStatus,
  boolean bound,
  boolean firstLogin
) {
}
