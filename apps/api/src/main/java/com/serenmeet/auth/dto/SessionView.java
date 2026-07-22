package com.serenmeet.auth.dto;

public record SessionView(
  String actorType,
  String subjectId,
  Long tenantId,
  String tenantStatus,
  boolean bound,
  boolean firstLogin,
  String nextPath
) {
}
