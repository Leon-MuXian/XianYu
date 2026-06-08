package com.serenmeet.audit.controller;

import com.serenmeet.audit.application.AuditApplicationService;
import com.serenmeet.audit.dto.AuditLogItem;
import com.serenmeet.common.PageResponse;
import com.serenmeet.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台操作记录接口。
 */
@RestController
@RequestMapping("/admin/audit-logs")
public class AdminAuditController {

  private final AuditApplicationService auditService;

  public AdminAuditController(AuditApplicationService auditService) {
    this.auditService = auditService;
  }

  @GetMapping
  public ApiResponse<PageResponse<AuditLogItem>> listAuditLogs(
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) String action,
    @RequestParam(required = false) String range,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int pageSize
  ) {
    return ApiResponse.ok(auditService.list(keyword, action, range, page, pageSize));
  }
}
