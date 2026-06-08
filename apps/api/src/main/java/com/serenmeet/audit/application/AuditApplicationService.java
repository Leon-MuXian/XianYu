package com.serenmeet.audit.application;

import com.serenmeet.audit.domain.AuditLogEntity;
import com.serenmeet.audit.domain.AuditActorType;
import com.serenmeet.audit.dto.AuditLogItem;
import com.serenmeet.audit.mapper.AuditLogMapper;
import com.serenmeet.common.PageResponse;
import com.serenmeet.common.PageQuery;
import org.springframework.stereotype.Service;

/**
 * 平台操作审计服务。
 */
@Service
public class AuditApplicationService {

  private final AuditLogMapper auditLogMapper;

  public AuditApplicationService(AuditLogMapper auditLogMapper) {
    this.auditLogMapper = auditLogMapper;
  }

  /**
   * 追加审计记录。
   */
  public void record(Long tenantId, String actorName, String action, String targetName, String oldValue, String newValue, String reason) {
    record(AuditActorType.ADMIN.code(), tenantId, actorName, action, targetName, oldValue, newValue, reason);
  }

  /**
   * 追加系统审计记录。
   */
  public void recordSystem(Long tenantId, String actorName, String action, String targetName, String oldValue, String newValue, String reason) {
    record(AuditActorType.SYSTEM.code(), tenantId, actorName, action, targetName, oldValue, newValue, reason);
  }

  private void record(String actorType, Long tenantId, String actorName, String action, String targetName, String oldValue, String newValue, String reason) {
    AuditLogEntity entity = new AuditLogEntity();
    entity.setTenantId(tenantId);
    entity.setActorType(actorType);
    entity.setActorName(actorName);
    entity.setAction(action);
    entity.setTargetName(targetName);
    entity.setOldValue(oldValue);
    entity.setNewValue(newValue);
    entity.setReason(reason);
    auditLogMapper.insert(entity);
  }

  /**
   * 分页查询审计记录。
   */
  public PageResponse<AuditLogItem> list(String keyword, String action, String range, int page, int pageSize) {
    PageQuery pageQuery = PageQuery.normalize(page, pageSize);
    long total = auditLogMapper.countAuditLogs(keyword, action, range);
    return new PageResponse<>(
      auditLogMapper.selectAuditLogs(keyword, action, range, pageQuery.pageSize(), pageQuery.offset()),
      total,
      pageQuery.page(),
      pageQuery.pageSize()
    );
  }
}
