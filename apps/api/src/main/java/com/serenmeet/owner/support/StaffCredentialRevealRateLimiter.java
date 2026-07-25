package com.serenmeet.owner.support;

import com.serenmeet.common.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** 店长按员工读取登录凭据的轻量短窗口限频。 */
@Component
public class StaffCredentialRevealRateLimiter {

  private final Map<String, RevealWindow> revealWindows = new ConcurrentHashMap<>();
  private final Clock clock;
  private final int maxReveals;
  private final Duration window;

  public StaffCredentialRevealRateLimiter(
      Clock clock,
      @Value("${seren-meet.staff-credential.max-reveals-per-window:10}") int maxReveals,
      @Value("${seren-meet.staff-credential.reveal-window-minutes:1}") int windowMinutes) {
    if (maxReveals < 1 || windowMinutes < 1) {
      throw new IllegalStateException("Staff credential reveal rate limit must be positive");
    }
    this.clock = clock;
    this.maxReveals = maxReveals;
    this.window = Duration.ofMinutes(windowMinutes);
  }

  public void acquire(String ownerSubjectId, Long staffId) {
    String key = ownerSubjectId + ":" + staffId;
    Instant now = Instant.now(clock);
    revealWindows.compute(key, (ignored, current) -> {
      if (current == null || current.isExpired(now, window)) {
        return new RevealWindow(now, 1);
      }
      if (current.count() >= maxReveals) {
        throw new ApiException(
            HttpStatus.TOO_MANY_REQUESTS,
            "STAFF_CREDENTIAL_REVEAL_RATE_LIMITED",
            "读取员工登录信息过于频繁，请稍后再试");
      }
      return new RevealWindow(current.startedAt(), current.count() + 1);
    });
  }

  private record RevealWindow(Instant startedAt, int count) {

    private boolean isExpired(Instant now, Duration window) {
      return startedAt.plus(window).isBefore(now);
    }
  }
}
