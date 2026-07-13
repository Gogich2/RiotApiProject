package org.main.refresh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.main.dto.CrawlResultDto;
import org.main.exception.ExternalServiceException;
import org.main.refresh.dto.PlayerRefreshStatusDto;
import org.main.refresh.entity.PlayerRefreshJobEntity;
import org.main.refresh.entity.RefreshSource;
import org.main.refresh.entity.RefreshState;
import org.main.refresh.repository.PlayerRefreshJobRepository;
import org.main.refresh.service.PlayerRefreshCoordinator;
import org.main.refresh.service.PlayerRefreshWorker;
import org.main.service.CrawlerService;
import org.main.service.RankEnrichmentResult;
import org.main.service.RankEnrichmentService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@ExtendWith(MockitoExtension.class)
class PlayerRefreshCoordinatorTest {

    private static final Instant INSTANT = Instant.parse("2026-07-13T06:00:00Z");

    private static final UUID JOB_ID = UUID.fromString("be88587f-04b1-49a9-a11a-89d76ddae08e");

    @Mock
    private PlayerRefreshJobRepository jobRepository;

    @Mock
    private PlayerRefreshWorker worker;

    @Mock
    private TaskExecutor taskExecutor;

    @Mock
    private CrawlerService crawlerService;

    @Mock
    private RankEnrichmentService rankEnrichmentService;

    private Clock clock;

    private PlayerRefreshCoordinator coordinator;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(INSTANT, ZoneOffset.UTC);
        coordinator = new PlayerRefreshCoordinator(
                jobRepository,
                worker,
                taskExecutor,
                clock,
                Duration.ofSeconds(120)
        );
    }

    @Test
    void enqueuesDurableJobBeforeSchedulingWorker() {
        when(jobRepository.findFirstByPuuidAndStateInOrderByRequestedAtDesc(any(), any())).
                thenReturn(Optional.empty());
        when(jobRepository.findFirstByPuuidAndSourceOrderByRequestedAtDesc("puuid", RefreshSource.MANUAL)).
                thenReturn(Optional.empty());
        when(jobRepository.insertQueued(any(), any(), any(), any())).thenReturn(1);

        PlayerRefreshStatusDto status = coordinator.enqueue("puuid", RefreshSource.MANUAL);

        assertThat(status.state()).isEqualTo(RefreshState.QUEUED);
        assertThat(status.requestedAt()).isEqualTo(OffsetDateTime.ofInstant(INSTANT, ZoneOffset.UTC));
        verify(jobRepository).insertQueued(any(), any(), any(), any());
        verify(taskExecutor).execute(any(Runnable.class));
    }

    @Test
    void returnsExistingActiveJobInsteadOfEnqueuingDuplicate() {
        PlayerRefreshJobEntity active = job(RefreshState.RUNNING, INSTANT.minusSeconds(5));
        when(jobRepository.findFirstByPuuidAndStateInOrderByRequestedAtDesc(any(), any())).
                thenReturn(Optional.of(active));

        PlayerRefreshStatusDto status = coordinator.enqueue("puuid", RefreshSource.MANUAL);

        assertThat(status.id()).isEqualTo(JOB_ID);
        assertThat(status.state()).isEqualTo(RefreshState.RUNNING);
        verify(jobRepository, never()).insertQueued(any(), any(), any(), any());
    }

    @Test
    void enforcesOneHundredTwentySecondManualCooldown() {
        when(jobRepository.findFirstByPuuidAndStateInOrderByRequestedAtDesc(any(), any())).
                thenReturn(Optional.empty());
        when(jobRepository.findFirstByPuuidAndSourceOrderByRequestedAtDesc("puuid", RefreshSource.MANUAL)).
                thenReturn(Optional.of(job(RefreshState.COMPLETED, INSTANT.minusSeconds(60))));

        assertThatThrownBy(() -> coordinator.enqueue("puuid", RefreshSource.MANUAL)).
                isInstanceOf(PlayerRefreshCoordinator.RefreshCooldownException.class).
                satisfies(exception -> assertThat(
                        ((PlayerRefreshCoordinator.RefreshCooldownException) exception).getRetryAfter()
                ).isEqualTo(Duration.ofSeconds(60)));
    }

    @Test
    void mapsLatestStoredStatus() {
        when(jobRepository.findFirstByPuuidOrderByRequestedAtDesc("puuid")).
                thenReturn(Optional.of(job(RefreshState.COMPLETED, INSTANT.minusSeconds(60))));

        assertThat(coordinator.latest("puuid").state()).isEqualTo(RefreshState.COMPLETED);
    }

    @Test
    void workerTransitionsToCompletedWithoutDeletingCachedData() {
        PlayerRefreshJobEntity job = job(RefreshState.QUEUED, INSTANT.minusSeconds(1));
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(crawlerService.crawlPuuidEUW("puuid", 20)).thenReturn(
                new CrawlResultDto("EUW1", "Player", "puuid", 20, 0, List.of())
        );
        when(rankEnrichmentService.enrichRanksForPuuidEuw("puuid")).thenReturn(
                new RankEnrichmentResult(List.of(), false)
        );
        PlayerRefreshWorker refreshWorker = new PlayerRefreshWorker(
                jobRepository,
                crawlerService,
                rankEnrichmentService,
                clock
        );

        refreshWorker.run(JOB_ID);

        assertThat(job.getState()).isEqualTo(RefreshState.COMPLETED);
        assertThat(job.getCompletedAt()).isEqualTo(OffsetDateTime.ofInstant(INSTANT, ZoneOffset.UTC));
        verify(crawlerService).crawlPuuidEUW("puuid", 20);
        verify(rankEnrichmentService).enrichRanksForPuuidEuw("puuid");
    }

    @Test
    void workerStoresSafeFailureMessage() {
        PlayerRefreshJobEntity job = job(RefreshState.QUEUED, INSTANT.minusSeconds(1));
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(crawlerService.crawlPuuidEUW("puuid", 20)).thenThrow(new RuntimeException("secret detail"));
        PlayerRefreshWorker refreshWorker = new PlayerRefreshWorker(
                jobRepository,
                crawlerService,
                rankEnrichmentService,
                clock
        );

        refreshWorker.run(JOB_ID);

        assertThat(job.getState()).isEqualTo(RefreshState.FAILED);
        assertThat(job.getUserMessage()).doesNotContain("secret detail");
    }

    @Test
    void workerMapsRiotRateLimitAndRetryAfter() {
        PlayerRefreshJobEntity job = job(RefreshState.QUEUED, INSTANT.minusSeconds(1));
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "45");
        HttpClientErrorException tooManyRequests = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "rate limited",
                headers,
                new byte[0],
                StandardCharsets.UTF_8
        );
        when(crawlerService.crawlPuuidEUW("puuid", 20)).
                thenThrow(new ExternalServiceException("Riot API failed", tooManyRequests));
        PlayerRefreshWorker refreshWorker = new PlayerRefreshWorker(
                jobRepository,
                crawlerService,
                rankEnrichmentService,
                clock
        );

        refreshWorker.run(JOB_ID);

        assertThat(job.getState()).isEqualTo(RefreshState.RATE_LIMITED);
        assertThat(job.getRetryAfter()).isEqualTo(
                OffsetDateTime.ofInstant(INSTANT.plusSeconds(45), ZoneOffset.UTC)
        );
    }

    private PlayerRefreshJobEntity job(RefreshState state, Instant requestedAt) {
        PlayerRefreshJobEntity job = new PlayerRefreshJobEntity();
        job.setId(JOB_ID);
        job.setPuuid("puuid");
        job.setSource(RefreshSource.MANUAL);
        job.setState(state);
        job.setRequestedAt(OffsetDateTime.ofInstant(requestedAt, ZoneOffset.UTC));
        return job;
    }
}
