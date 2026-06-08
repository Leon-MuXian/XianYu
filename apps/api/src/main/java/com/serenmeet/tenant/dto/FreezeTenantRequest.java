package com.serenmeet.tenant.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

/**
 * 冻结租户二次确认请求。
 */
public record FreezeTenantRequest(
  @NotBlank(message = "请填写冻结原因") String reason,
  @AssertTrue(message = "请勾选确认暂停三端业务操作") Boolean confirmed
) {
}
