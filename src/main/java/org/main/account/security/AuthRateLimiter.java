package org.main.account.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthRateLimiter {

    private final Clock clock;

    private final int maxAttempts;

    private final Duration window;

    private final int maxTrackedKeys;

    private final Map<AttemptKey, Deque<Instant>> attempts = new LinkedHashMap<>(16, 0.75f, true);

    public AuthRateLimiter(
            Clock clock,
            @Value("${app.auth.rate-limit-attempts:5}") int maxAttempts,
            @Value("${app.auth.rate-limit-window:10m}") Duration window,
            @Value("${app.auth.rate-limit-max-keys:10000}") int maxTrackedKeys
    ) {
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.maxTrackedKeys = maxTrackedKeys;
    }

    public synchronized void check(String clientIp, String action) {
        AttemptKey key = new AttemptKey(
                Objects.requireNonNull(clientIp, "clientIp"),
                Objects.requireNonNull(action, "action")
        );
        Instant now = clock.instant();
        Deque<Instant> actionAttempts = attempts.get(key);
        if (actionAttempts == null) {
            evictOldestKeyIfFull();
            actionAttempts = new ArrayDeque<>();
            attempts.put(key, actionAttempts);
        }

        Instant cutoff = now.minus(window);
        while (!actionAttempts.isEmpty() && !actionAttempts.peekFirst().isAfter(cutoff)) {
            actionAttempts.removeFirst();
        }
        if (actionAttempts.size() >= maxAttempts) {
            Duration retryAfter = Duration.between(now, actionAttempts.peekFirst().plus(window));
            throw new AuthRateLimitException(retryAfter);
        }
        actionAttempts.addLast(now);
    }

    private void evictOldestKeyIfFull() {
        if (attempts.size() >= maxTrackedKeys) {
            AttemptKey oldestKey = attempts.keySet().iterator().next();
            attempts.remove(oldestKey);
        }
    }

    private record AttemptKey(String clientIp, String action) {
    }

    public static class AuthRateLimitException extends RuntimeException {

        private final Duration retryAfter;

        public AuthRateLimitException(Duration retryAfter) {
            super("Too many authentication attempts. Please try again later.");
            this.retryAfter = retryAfter;
        }

        public Duration getRetryAfter() {
            return retryAfter;
        }
    }
}
