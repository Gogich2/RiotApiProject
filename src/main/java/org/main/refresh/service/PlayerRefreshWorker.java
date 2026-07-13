package org.main.refresh.service;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.main.refresh.entity.PlayerRefreshJobEntity;
import org.main.refresh.entity.RefreshState;
import org.main.refresh.repository.PlayerRefreshJobRepository;
import org.main.service.CrawlerService;
import org.main.service.RankEnrichmentService;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

@Service
public class PlayerRefreshWorker {

    private static final Duration DEFAULT_RETRY_AFTER = Duration.ofMinutes(2);

    private final PlayerRefreshJobRepository jobRepository;

    private final CrawlerService crawlerService;

    private final RankEnrichmentService rankEnrichmentService;

    private final Clock clock;

    public PlayerRefreshWorker(
            PlayerRefreshJobRepository jobRepository,
            CrawlerService crawlerService,
            RankEnrichmentService rankEnrichmentService,
            Clock clock
    ) {
        this.jobRepository = jobRepository;
        this.crawlerService = crawlerService;
        this.rankEnrichmentService = rankEnrichmentService;
        this.clock = clock;
    }

    public void run(UUID jobId) {
        PlayerRefreshJobEntity job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.getState() != RefreshState.QUEUED) {
            return;
        }
        job.setState(RefreshState.RUNNING);
        job.setStartedAt(now());
        jobRepository.save(job);
        try {
            crawlerService.crawlPuuidEUW(job.getPuuid(), 20);
            rankEnrichmentService.enrichRanksForPuuidEuw(job.getPuuid());
            job.setState(RefreshState.COMPLETED);
            job.setUserMessage("Player data is up to date.");
        } catch (RuntimeException exception) {
            HttpStatusCodeException rateLimit = findRateLimit(exception);
            if (rateLimit != null) {
                job.setState(RefreshState.RATE_LIMITED);
                job.setFailureCategory("RIOT_RATE_LIMIT");
                job.setRetryAfter(now().plus(retryAfter(rateLimit)));
                job.setUserMessage("Riot is rate limiting updates. Cached data is still available.");
            } else {
                job.setState(RefreshState.FAILED);
                job.setFailureCategory("REFRESH_FAILED");
                job.setUserMessage("The update failed. Cached data is still available.");
            }
        }
        job.setCompletedAt(now());
        jobRepository.save(job);
    }

    private HttpStatusCodeException findRateLimit(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof HttpStatusCodeException statusException
                    && statusException.getStatusCode().value() == 429) {
                return statusException;
            }
            current = current.getCause();
        }
        return null;
    }

    private Duration retryAfter(HttpStatusCodeException exception) {
        String header = exception.getResponseHeaders() == null
                ? null : exception.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        if (header == null) {
            return DEFAULT_RETRY_AFTER;
        }
        try {
            return Duration.ofSeconds(Math.max(1, Long.parseLong(header)));
        } catch (NumberFormatException ignored) {
            return DEFAULT_RETRY_AFTER;
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
