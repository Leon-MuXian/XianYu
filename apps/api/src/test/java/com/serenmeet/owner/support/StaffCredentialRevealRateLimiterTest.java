package com.serenmeet.owner.support;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.serenmeet.common.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class StaffCredentialRevealRateLimiterTest {

  @Test
  void limitsEachOwnerAndStaffPairIndependently() {
    StaffCredentialRevealRateLimiter limiter = new StaffCredentialRevealRateLimiter(
      Clock.fixed(Instant.parse("2026-07-25T10:00:00Z"), ZoneOffset.UTC), 2, 1
    );

    limiter.acquire("owner-1", 10L);
    limiter.acquire("owner-1", 10L);
    limiter.acquire("owner-1", 11L);
    limiter.acquire("owner-2", 10L);

    assertThatThrownBy(() -> limiter.acquire("owner-1", 10L))
      .isInstanceOfSatisfying(ApiException.class, exception ->
        org.assertj.core.api.Assertions.assertThat(exception.code())
          .isEqualTo("STAFF_CREDENTIAL_REVEAL_RATE_LIMITED")
      );
  }
}
