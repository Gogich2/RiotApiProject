package org.main.client;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RiotRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RiotRateLimiter.class);

    private final boolean enabled;

    private final int perSecondLimit;

    private final int perTwoMinutesLimit;

    private final Deque<Instant> secondWindow = new ArrayDeque<>();

    private final Deque<Instant> twoMinuteWindow = new ArrayDeque<>();

    public RiotRateLimiter(
            @Value("${riot.rate-limit.enabled:true}") boolean enabled,
            @Value("${riot.rate-limit.per-second:5}") int perSecondLimit,
            @Value("${riot.rate-limit.per-two-minutes:70}") int perTwoMinutesLimit
    ) {
        this.enabled = enabled;
        this.perSecondLimit = Math.max(1, perSecondLimit);
        this.perTwoMinutesLimit = Math.max(1, perTwoMinutesLimit);
    }

    public synchronized void acquire() {
        if (!enabled) {
            return;
        }

        while (true) {
            Instant now = Instant.now();

            removeOldEntries(secondWindow, now.minusSeconds(1));
            removeOldEntries(twoMinuteWindow, now.minusSeconds(120));

            long waitForSecondWindow = calculateWaitMillis(
                    secondWindow,
                    perSecondLimit,
                    Duration.ofSeconds(1),
                    now
            );

            long waitForTwoMinuteWindow = calculateWaitMillis(
                    twoMinuteWindow,
                    perTwoMinutesLimit,
                    Duration.ofSeconds(120),
                    now
            );

            long waitMillis = Math.max(waitForSecondWindow, waitForTwoMinuteWindow);

            if (waitMillis <= 0) {
                secondWindow.addLast(now);
                twoMinuteWindow.addLast(now);
                return;
            }

            sleep(waitMillis);
        }
    }

    public void pauseAfterRetryAfterHeader(String retryAfterHeader) {
        long seconds = parseRetryAfterSeconds(retryAfterHeader);

        if (seconds <= 0) {
            seconds = 10;
        }

        long millis = seconds * 1000L;

        log.warn("Riot API returned 429. Pausing requests for {} ms", millis);
        sleep(millis);
    }

    private void removeOldEntries(Deque<Instant> window, Instant threshold) {
        while (!window.isEmpty() && window.peekFirst().isBefore(threshold)) {
            window.removeFirst();
        }
    }

    private long calculateWaitMillis(Deque<Instant> window,
                                     int limit,
                                     Duration duration,
                                     Instant now) {
        if (window.size() < limit) {
            return 0;
        }

        Instant oldest = window.peekFirst();

        if (oldest == null) {
            return 0;
        }

        Instant allowedAt = oldest.plus(duration);
        long waitMillis = Duration.between(now, allowedAt).toMillis();

        return Math.max(waitMillis + 100, 0);
    }

    private long parseRetryAfterSeconds(String retryAfterHeader) {
        if (retryAfterHeader == null || retryAfterHeader.isBlank()) {
            return 0;
        }

        try {
            return Long.parseLong(retryAfterHeader.trim());
        } catch (NumberFormatException ex) {
            log.warn("Cannot parse Retry-After header: '{}'", retryAfterHeader);
            return 0;
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(Math.max(millis, 1));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Rate limiter was interrupted", ex);
        }
    }
}