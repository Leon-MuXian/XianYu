package com.serenmeet.auth.support;

import com.serenmeet.common.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * MVP 后台登录短窗口限流。
 */
@Component
public class AdminLoginRateLimiter {

  private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();
  private final Clock clock;
  private final int maxFailures;
  private final Duration window;

  public AdminLoginRateLimiter(
    Clock clock,
    @Value("${seren-meet.admin-auth.max-login-failures:5}") int maxFailures,
    @Value("${seren-meet.admin-auth.failure-window-minutes:10}") int windowMinutes
  ) {
    this.clock = clock;
    this.maxFailures = maxFailures;
    this.window = Duration.ofMinutes(windowMinutes);
  }

  public void checkAllowed(String username) {
    AttemptWindow attemptWindow = attempts.get(normalize(username));
    if (attemptWindow == null || attemptWindow.isExpired(Instant.now(clock), window)) {
      return;
    }
    if (attemptWindow.failures >= maxFailures) {
      throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "LOGIN_RATE_LIMITED", "登录失败次数过多，请稍后再试");
    }
  }

  public void recordFailure(String username) {
    Instant now = Instant.now(clock);
    attempts.compute(normalize(username), (key, oldWindow) -> {
      if (oldWindow == null || oldWindow.isExpired(now, window)) {
        return new AttemptWindow(now, 1);
      }
      return new AttemptWindow(oldWindow.startedAt, oldWindow.failures + 1);
    });
  }

  public void recordSuccess(String username) {
    attempts.remove(normalize(username));
  }

  private String normalize(String username) {
    return username == null ? "" : username.trim().toLowerCase();
  }

  private record AttemptWindow(Instant startedAt, int failures) {

    private boolean isExpired(Instant now, Duration window) {
      return startedAt.plus(window).isBefore(now);
    }
  }
}
