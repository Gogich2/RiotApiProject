package org.main.account.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthRateLimiterTest {

    private MutableClock clock;

    private AuthRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-07-13T02:30:00Z"));
        rateLimiter = new AuthRateLimiter(clock, 5, Duration.ofMinutes(10), 100);
    }

    @Test
    void rejectsSixthAttemptForSameIpAndAction() {
        for (int attempt = 0; attempt < 5; attempt++) {
            rateLimiter.check("203.0.113.10", "login");
        }

        assertThatThrownBy(() -> rateLimiter.check("203.0.113.10", "login")).
                isInstanceOf(AuthRateLimiter.AuthRateLimitException.class);
    }

    @Test
    void separatesActionsAndExpiresOldAttempts() {
        for (int attempt = 0; attempt < 5; attempt++) {
            rateLimiter.check("203.0.113.10", "login");
        }

        assertThatCode(() -> rateLimiter.check("203.0.113.10", "register")).doesNotThrowAnyException();
        clock.advance(Duration.ofMinutes(10).plusSeconds(1));
        assertThatCode(() -> rateLimiter.check("203.0.113.10", "login")).doesNotThrowAnyException();
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
