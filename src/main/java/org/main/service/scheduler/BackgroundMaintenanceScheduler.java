package org.main.service.scheduler;

import java.time.Duration;
import java.time.Instant;
import org.main.client.RiotRateLimiter;
import org.main.dto.CrawlResultDto;
import org.main.dto.DataIntegrityReportDto;
import org.main.service.CrawlerService;
import org.main.service.DataIntegrityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.scheduler.background-maintenance.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class BackgroundMaintenanceScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(BackgroundMaintenanceScheduler.class);

    private final DataIntegrityService dataIntegrityService;

    private final CrawlerService crawlerService;

    private final RiotRateLimiter riotRateLimiter;

    private final int integritySharePercent;

    private final int headroomRequests;

    public BackgroundMaintenanceScheduler(
            DataIntegrityService dataIntegrityService,
            CrawlerService crawlerService,
            RiotRateLimiter riotRateLimiter,
            @Value("${app.scheduler.background-maintenance.integrity-share-percent:50}")
            int integritySharePercent,
            @Value("${app.scheduler.background-maintenance.headroom-requests:1}")
            int headroomRequests
    ) {
        this.dataIntegrityService = dataIntegrityService;
        this.crawlerService = crawlerService;
        this.riotRateLimiter = riotRateLimiter;
        this.integritySharePercent = integritySharePercent;
        this.headroomRequests = headroomRequests;
    }

    @Scheduled(
            fixedDelayString = "${app.scheduler.background-maintenance.fixed-delay-ms:120000}",
            initialDelayString = "${app.scheduler.background-maintenance.initial-delay-ms:60000}"
    )
    public void runMaintenanceCycle() {
        Instant startedAt = Instant.now();
        int initialBudget = availableBackgroundRequests();

        if (initialBudget == 0) {
            IntegrityOutcome localIntegrity = runStoredTimelineIntegrity();
            logCycleSummary(
                    startedAt,
                    IntegrityOutcome.success(0),
                    CrawlOutcome.empty(0),
                    localIntegrity
            );
            return;
        }

        int protectedIntegrityBudget = MaintenanceBudget.integrityProtectedBudget(
                initialBudget,
                integritySharePercent
        );

        int protectedCrawlerBudget = initialBudget - protectedIntegrityBudget;
        IntegrityOutcome protectedIntegrity = runProtectedIntegrity(protectedIntegrityBudget);
        CrawlOutcome crawl = runCrawlerWithAvailableCapacity(protectedCrawlerBudget);
        IntegrityOutcome borrowedIntegrity = runBorrowedIntegrityWithAvailableCapacity();

        logCycleSummary(startedAt, protectedIntegrity, crawl, borrowedIntegrity);
    }

    private IntegrityOutcome runStoredTimelineIntegrity() {
        try {
            dataIntegrityService.repairStoredTimelineData(50);
            return IntegrityOutcome.success(0);
        } catch (Exception ex) {
            log.error("Stored timeline data integrity phase failed", ex);
            return IntegrityOutcome.failure();
        }
    }

    private void logCycleSummary(
            Instant startedAt,
            IntegrityOutcome protectedIntegrity,
            CrawlOutcome crawl,
            IntegrityOutcome borrowedIntegrity
    ) {
        log.info(
                "Background maintenance cycle finished: protectedTimelinesRepaired={}, "
                        + "borrowedTimelinesRepaired={}, crawlerPuuid='{}', "
                        + "savedNewMatches={}, crawlerExtraBudget={}, failures={}, durationMs={}",
                protectedIntegrity.repairedRawTimelines(),
                borrowedIntegrity.repairedRawTimelines(),
                crawl.puuid(),
                crawl.savedNewMatches(),
                crawl.extraBudgetOffered(),
                (protectedIntegrity.failed() ? 1 : 0)
                        + (crawl.failed() ? 1 : 0)
                        + (borrowedIntegrity.failed() ? 1 : 0),
                Duration.between(startedAt, Instant.now()).toMillis()
        );
    }

    private IntegrityOutcome runProtectedIntegrity(int requestBudget) {
        if (requestBudget <= 0) {
            return IntegrityOutcome.success(0);
        }

        try {
            DataIntegrityReportDto before = dataIntegrityService.check();

            if (before.valid()) {
                return IntegrityOutcome.success(0);
            }

            DataIntegrityReportDto after =
                    dataIntegrityService.repairMissingTimelines(requestBudget);
            long repaired = Math.max(
                    0,
                    before.matchesWithoutTimelineRaw() - after.matchesWithoutTimelineRaw()
            );
            return IntegrityOutcome.success(repaired);
        } catch (Exception ex) {
            log.error("Protected data integrity phase failed", ex);
            return IntegrityOutcome.failure();
        }
    }

    private CrawlOutcome runCrawlerWithAvailableCapacity(int protectedCrawlerBudget) {
        int availableRequests = availableBackgroundRequests();
        int extraBudgetOffered = Math.max(0, availableRequests - protectedCrawlerBudget);
        int matchLimit = MaintenanceBudget.maxCrawlerMatches(availableRequests);

        if (matchLimit == 0) {
            return CrawlOutcome.empty(0);
        }

        try {
            var result = crawlerService.crawlNextPlayerEUW(matchLimit);

            if (result.isEmpty()) {
                log.debug("Background crawl skipped because no player is stored");
                return CrawlOutcome.empty(0);
            }

            CrawlResultDto crawl = result.get();
            return new CrawlOutcome(
                    crawl.puuid(),
                    crawl.savedNewMatches(),
                    extraBudgetOffered,
                    false
            );
        } catch (Exception ex) {
            log.error("Background crawler phase failed", ex);
            return CrawlOutcome.failure(extraBudgetOffered);
        }
    }

    private IntegrityOutcome runBorrowedIntegrityWithAvailableCapacity() {
        try {
            DataIntegrityReportDto before = dataIntegrityService.check();
            int requestBudget = availableBackgroundRequests();
            int repairLimit = (int) Math.min(
                    before.matchesWithoutTimelineRaw(),
                    (long) requestBudget
            );

            if (repairLimit == 0) {
                return IntegrityOutcome.success(0);
            }

            DataIntegrityReportDto after = dataIntegrityService.repairMissingTimelines(repairLimit);
            long repaired = Math.max(
                    0,
                    before.matchesWithoutTimelineRaw() - after.matchesWithoutTimelineRaw()
            );
            return IntegrityOutcome.success(repaired);
        } catch (Exception ex) {
            log.error("Borrowed data integrity phase failed", ex);
            return IntegrityOutcome.failure();
        }
    }

    private int availableBackgroundRequests() {
        return MaintenanceBudget.cycleBudget(
                riotRateLimiter.getPerTwoMinuteLimit(),
                riotRateLimiter.remainingTwoMinuteCapacity(),
                headroomRequests
        );
    }

    private record IntegrityOutcome(long repairedRawTimelines, boolean failed) {

        private static IntegrityOutcome success(long repairedRawTimelines) {
            return new IntegrityOutcome(repairedRawTimelines, false);
        }

        private static IntegrityOutcome failure() {
            return new IntegrityOutcome(0, true);
        }
    }

    private record CrawlOutcome(
            String puuid,
            int savedNewMatches,
            int extraBudgetOffered,
            boolean failed
    ) {

        private static CrawlOutcome empty(int extraBudgetOffered) {
            return new CrawlOutcome(null, 0, extraBudgetOffered, false);
        }

        private static CrawlOutcome failure(int extraBudgetOffered) {
            return new CrawlOutcome(null, 0, extraBudgetOffered, true);
        }
    }
}
