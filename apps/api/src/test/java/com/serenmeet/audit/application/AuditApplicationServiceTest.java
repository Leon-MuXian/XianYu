package com.serenmeet.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.serenmeet.audit.mapper.AuditLogMapper;
import com.serenmeet.common.PageResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditApplicationServiceTest {

  @Mock
  private AuditLogMapper auditLogMapper;

  private AuditApplicationService service;

  @BeforeEach
  void setUp() {
    service = new AuditApplicationService(auditLogMapper);
  }

  @Test
  void listNormalizesPagination() {
    when(auditLogMapper.countAuditLogs(null, null, null)).thenReturn(0L);
    when(auditLogMapper.selectAuditLogs(null, null, null, 50, 0)).thenReturn(List.of());

    PageResponse<?> response = service.list(null, null, null, -3, 500);

    assertThat(response.page()).isEqualTo(1);
    assertThat(response.pageSize()).isEqualTo(50);
  }
}
