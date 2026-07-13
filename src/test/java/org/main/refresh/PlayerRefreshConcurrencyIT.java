package org.main.refresh;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.main.refresh.entity.RefreshSource;
import org.main.refresh.service.PlayerRefreshCoordinator;
import org.main.refresh.service.PlayerRefreshWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
class PlayerRefreshConcurrencyIT {

    private static final int REQUESTS = 12;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private PlayerRefreshCoordinator coordinator;

    @MockBean
    private PlayerRefreshWorker worker;

    @Test
    void concurrentEnqueueReturnsOneDurableActiveJob() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(REQUESTS);
        CountDownLatch ready = new CountDownLatch(REQUESTS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<UUID>> results = new ArrayList<>();

        try {
            for (int index = 0; index < REQUESTS; index++) {
                results.add(executor.submit(() -> enqueue(ready, start)));
            }
            ready.await();
            start.countDown();

            Set<UUID> jobIds = new HashSet<>();
            for (Future<UUID> result : results) {
                jobIds.add(result.get());
            }
            assertThat(jobIds).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private UUID enqueue(CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        return coordinator.enqueue("concurrent-puuid", RefreshSource.SCHEDULED).id();
    }
}
