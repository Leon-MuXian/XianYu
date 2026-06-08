package com.serenmeet.auth.support;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.serenmeet.common.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class AdminLoginRateLimiterTest {

  @Test
  void rejectsAfterConfiguredFailuresInWindow() {
    AdminLoginRateLimiter limiter = new AdminLoginRateLimiter(
      Clock.fixed(Instant.parse("2026-06-07T10:00:00Z"), ZoneId.of("Asia/Shanghai")),
      2,
      10
    );

    limiter.recordFailure("admin@serenmeet");
    limiter.recordFailure("admin@serenmeet");

    assertThatThrownBy(() -> limiter.checkAllowed("ADMIN@serenmeet"))
      .isInstanceOf(ApiException.class)
      .hasMessage("登录失败次数过多，请稍后再试");
  }
}
