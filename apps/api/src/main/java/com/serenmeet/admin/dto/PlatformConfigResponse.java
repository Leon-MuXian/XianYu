package com.serenmeet.admin.dto;

/**
 * 平台运营配置响应。
 */
public record PlatformConfigResponse(
  String configKey,
  String name,
  String valueText,
  String valueType,
  String unit,
  Integer minValue,
  Integer maxValue,
  String impactScope,
  Boolean editable,
  Boolean enabled
) {
}
