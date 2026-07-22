package com.serenmeet.admin.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.serenmeet.admin.domain.SupportWechatEntity;
import com.serenmeet.auth.dto.AdminUserView;
import com.serenmeet.admin.dto.SupportWechatResponse;
import com.serenmeet.admin.dto.UpdateSupportWechatRequest;
import com.serenmeet.admin.mapper.SupportWechatMapper;
import com.serenmeet.audit.application.AuditApplicationService;
import com.serenmeet.audit.domain.AuditAction;
import com.serenmeet.common.ApiException;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 平台客服微信配置服务。
 */
@Service
public class SupportWechatApplicationService {

  private final SupportWechatMapper supportWechatMapper;
  private final AuditApplicationService auditService;
  private final Clock clock;

  public SupportWechatApplicationService(SupportWechatMapper supportWechatMapper, AuditApplicationService auditService, Clock clock) {
    this.supportWechatMapper = supportWechatMapper;
    this.auditService = auditService;
    this.clock = clock;
  }

  /**
   * 读取默认客服微信配置。
   */
  public SupportWechatResponse getDefaultSupportWechat() {
    SupportWechatEntity entity = defaultEntity();
    return toResponse(entity);
  }

  /**
   * 保存默认客服微信文本配置。
   */
  @Transactional
  public SupportWechatResponse updateDefaultSupportWechat(UpdateSupportWechatRequest request, AdminUserView actor) {
    SupportWechatEntity entity = defaultEntity();
    String oldValue = entity.getWechatId() + " / " + entity.getDisplayText();
    entity.setWechatId(request.wechatId());
    entity.setDisplayText(request.displayText());
    entity.setDisplayScope(request.displayScope());
    entity.setEnabled(request.enabled() == null || request.enabled());
    entity.setUpdatedAt(OffsetDateTime.now(clock));
    supportWechatMapper.updateById(entity);
    auditService.record(null, actor.username(), AuditAction.UPDATE_SUPPORT_WECHAT.code(), "默认客服", oldValue, entity.getWechatId(), request.reason());
    return toResponse(entity);
  }

  private SupportWechatEntity defaultEntity() {
    SupportWechatEntity entity = supportWechatMapper.selectOne(
      new LambdaQueryWrapper<SupportWechatEntity>().isNull(SupportWechatEntity::getTenantId).last("limit 1")
    );
    if (entity == null) {
      throw new ApiException(HttpStatus.NOT_FOUND, "SUPPORT_WECHAT_NOT_CONFIGURED", "默认客服微信未配置");
    }
    return entity;
  }

  private SupportWechatResponse toResponse(SupportWechatEntity entity) {
    return new SupportWechatResponse(
      entity.getId(),
      entity.getWechatId(),
      entity.getDisplayText(),
      entity.getDisplayScope(),
      entity.getEnabled()
    );
  }
}
