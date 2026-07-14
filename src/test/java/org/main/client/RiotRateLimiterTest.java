package org.main.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RiotRateLimiterTest {

    @Test
    void reportsConfiguredAndRemainingTwoMinuteCapacity() {
        RiotRateLimiter limiter = new RiotRateLimiter(true, 5, 85);

        assertThat(limiter.getPerTwoMinuteLimit()).isEqualTo(85);
        assertThat(limiter.remainingTwoMinuteCapacity()).isEqualTo(85);

        limiter.acquire();

        assertThat(limiter.remainingTwoMinuteCapacity()).isEqualTo(84);
    }

    @Test
    void disabledLimiterReportsItsConfiguredCapacity() {
        RiotRateLimiter limiter = new RiotRateLimiter(false, 5, 85);

        limiter.acquire();

        assertThat(limiter.remainingTwoMinuteCapacity()).isEqualTo(85);
    }
}
