package com.serenmeet.tenant.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 调整期限并解冻请求。
 */
public record ExtendTenantRequest(
  @NotNull(message = "请选择新到期日") @FutureOrPresent(message = "新到期日不能早于今天") LocalDate newTrialEndAt,
  @NotBlank(message = "请填写操作原因") String reason,
  String internalNote
) {
}
