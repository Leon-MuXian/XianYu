package com.serenmeet.admin.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.serenmeet.audit.application.AuditApplicationService;
import com.serenmeet.admin.mapper.SupportWechatMapper;
import com.serenmeet.common.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupportWechatApplicationServiceTest {

  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-07T10:00:00Z"), ZoneId.of("Asia/Shanghai"));

  @Mock
  private SupportWechatMapper supportWechatMapper;

  @Mock
  private AuditApplicationService auditService;

  private SupportWechatApplicationService service;

  @BeforeEach
  void setUp() {
    service = new SupportWechatApplicationService(supportWechatMapper, auditService, CLOCK);
  }

  @Test
  void getDefaultSupportWechatReturnsControlledErrorWhenMissing() {
    when(supportWechatMapper.selectOne(any())).thenReturn(null);

    assertThatThrownBy(() -> service.getDefaultSupportWechat())
      .isInstanceOf(ApiException.class)
      .hasMessage("默认客服微信未配置");
  }
}
