package com.serenmeet.admin.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.serenmeet.admin.domain.PlatformConfigEntity;
import com.serenmeet.auth.dto.AdminUserView;
import com.serenmeet.admin.dto.PlatformConfigResponse;
import com.serenmeet.admin.dto.UpdatePlatformConfigRequest;
import com.serenmeet.admin.mapper.PlatformConfigMapper;
import com.serenmeet.audit.application.AuditApplicationService;
import com.serenmeet.audit.domain.AuditAction;
import com.serenmeet.common.ApiException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 平台运营配置服务。
 */
@Service
public class PlatformConfigApplicationService {

  private final PlatformConfigMapper platformConfigMapper;
  private final AuditApplicationService auditService;
  private final Clock clock;

  public PlatformConfigApplicationService(PlatformConfigMapper platformConfigMapper, AuditApplicationService auditService, Clock clock) {
    this.platformConfigMapper = platformConfigMapper;
    this.auditService = auditService;
    this.clock = clock;
  }

  /**
   * 查询平台配置列表。
   */
  public List<PlatformConfigResponse> listConfigs(String keyword, Boolean editableOnly) {
    LambdaQueryWrapper<PlatformConfigEntity> wrapper = new LambdaQueryWrapper<PlatformConfigEntity>()
      .orderByAsc(PlatformConfigEntity::getConfigKey);
    if (keyword != null && !keyword.isBlank()) {
      wrapper.and(query -> query.like(PlatformConfigEntity::getConfigKey, keyword.trim())
        .or()
        .like(PlatformConfigEntity::getName, keyword.trim()));
    }
    if (Boolean.TRUE.equals(editableOnly)) {
      wrapper.eq(PlatformConfigEntity::getEditable, true);
    }
    return platformConfigMapper.selectList(wrapper).stream().map(this::toResponse).toList();
  }

  /**
   * 修改配置并写入审计。
   */
  @Transactional
  public PlatformConfigResponse updateConfig(String configKey, UpdatePlatformConfigRequest request, AdminUserView actor) {
    PlatformConfigEntity entity = platformConfigMapper.selectById(configKey);
    if (entity == null) {
      throw new ApiException(HttpStatus.NOT_FOUND, "CONFIG_NOT_FOUND", "配置项不存在");
    }
    if (!Boolean.TRUE.equals(entity.getEditable())) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "CONFIG_NOT_EDITABLE", "该配置项不可编辑");
    }
    if (!Boolean.TRUE.equals(entity.getEnabled())) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "CONFIG_DISABLED", "该配置项已停用");
    }
    validateValue(entity, request.newValue());
    String oldValue = entity.getValueText();
    entity.setValueText(request.newValue());
    entity.setUpdatedAt(LocalDateTime.now(clock));
    platformConfigMapper.updateById(entity);
    auditService.record(null, actor.username(), AuditAction.UPDATE_CONFIG.code(), entity.getName(), oldValue, request.newValue(), request.reason());
    return toResponse(entity);
  }

  private void validateValue(PlatformConfigEntity entity, String newValue) {
    if (!"INTEGER".equals(entity.getValueType())) {
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CONFIG_VALUE_TYPE_UNSUPPORTED", "配置项类型暂不支持");
    }
    try {
      int value = Integer.parseInt(newValue);
      if (entity.getMinValue() != null && value < entity.getMinValue()) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "CONFIG_VALUE_TOO_SMALL", "新值不能小于 " + entity.getMinValue());
      }
      if (entity.getMaxValue() != null && value > entity.getMaxValue()) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "CONFIG_VALUE_TOO_LARGE", "新值不能大于 " + entity.getMaxValue());
      }
    } catch (NumberFormatException exception) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "CONFIG_VALUE_TYPE_ERROR", "新值必须是整数");
    }
  }

  private PlatformConfigResponse toResponse(PlatformConfigEntity entity) {
    return new PlatformConfigResponse(
      entity.getConfigKey(),
      entity.getName(),
      entity.getValueText(),
      entity.getValueType(),
      entity.getUnit(),
      entity.getMinValue(),
      entity.getMaxValue(),
      entity.getImpactScope(),
      entity.getEditable(),
      entity.getEnabled()
    );
  }
}
