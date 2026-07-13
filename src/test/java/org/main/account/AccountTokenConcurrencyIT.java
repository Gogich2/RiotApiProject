package org.main.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.main.account.entity.AccountTokenType;
import org.main.account.entity.AppUserEntity;
import org.main.account.entity.AppUserStatus;
import org.main.account.repository.AppUserRepository;
import org.main.account.service.AccountTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DirtiesContext
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "app.scheduler.data-integrity.enabled=false",
        "app.scheduler.match-analysis.enabled=false"
})
class AccountTokenConcurrencyIT {

    private static final int CONSUMERS = 12;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private AccountTokenService tokenService;

    @Autowired
    private AppUserRepository userRepository;

    @Test
    void actionTokenCanBeConsumedOnlyOnceUnderConcurrency() throws Exception {
        AppUserEntity user = userRepository.saveAndFlush(user());
        String token = tokenService.issue(user.getId(), AccountTokenType.PASSWORD_RESET);
        ExecutorService executor = Executors.newFixedThreadPool(CONSUMERS);
        CountDownLatch ready = new CountDownLatch(CONSUMERS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        try {
            for (int index = 0; index < CONSUMERS; index++) {
                results.add(executor.submit(() -> consume(token, ready, start)));
            }
            ready.await();
            start.countDown();

            long successfulConsumes = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    successfulConsumes++;
                }
            }
            assertThat(successfulConsumes).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean consume(String token, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            tokenService.consume(token, AccountTokenType.PASSWORD_RESET);
            return true;
        } catch (AccountTokenService.InvalidAccountTokenException exception) {
            return false;
        }
    }

    private AppUserEntity user() {
        OffsetDateTime now = OffsetDateTime.now();
        AppUserEntity user = new AppUserEntity();
        user.setId(UUID.randomUUID());
        user.setEmailNormalized("token-race@example.com");
        user.setDisplayName("Token race");
        user.setStatus(AppUserStatus.ACTIVE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return user;
    }
}
