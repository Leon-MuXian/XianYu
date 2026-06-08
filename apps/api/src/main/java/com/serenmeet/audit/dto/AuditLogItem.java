package com.serenmeet.audit.dto;

import java.time.LocalDateTime;

/**
 * 平台操作记录列表行。
 */
public record AuditLogItem(
  Long id,
  Long tenantId,
  String actorType,
  String actorName,
  String action,
  String targetName,
  String oldValue,
  String newValue,
  String reason,
  LocalDateTime createdAt
) {
}
