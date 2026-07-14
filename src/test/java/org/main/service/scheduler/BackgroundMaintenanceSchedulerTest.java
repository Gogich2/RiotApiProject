package org.main.service.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.main.client.RiotRateLimiter;
import org.main.dto.CrawlResultDto;
import org.main.dto.DataIntegrityReportDto;
import org.main.service.CrawlerService;
import org.main.service.DataIntegrityService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class BackgroundMaintenanceSchedulerTest {

    @Test
    void zeroCapacityRepairsStoredTimelineDataAndLogsCycleSummary(CapturedOutput output) {
        DataIntegrityService integrity = mock(DataIntegrityService.class);
        CrawlerService crawler = mock(CrawlerService.class);
        RiotRateLimiter limiter = mock(RiotRateLimiter.class);
        BackgroundMaintenanceScheduler scheduler =
                new BackgroundMaintenanceScheduler(integrity, crawler, limiter, 50, 1);
        when(limiter.getPerTwoMinuteLimit()).thenReturn(85);
        when(limiter.remainingTwoMinuteCapacity()).thenReturn(1);

        scheduler.runMaintenanceCycle();

        verify(integrity).repairStoredTimelineData(50);
        verify(integrity, never()).repairMissingTimelines(org.mockito.ArgumentMatchers.anyInt());
        verifyNoInteractions(crawler);
        assertThat(output).contains("Background maintenance cycle finished:");
    }

    @Test
    void usesProtectedIntegrityHalfThenCrawlerAndBorrowedIntegrity() {
        DataIntegrityService integrity = mock(DataIntegrityService.class);
        CrawlerService crawler = mock(CrawlerService.class);
        RiotRateLimiter limiter = mock(RiotRateLimiter.class);
        BackgroundMaintenanceScheduler scheduler =
                new BackgroundMaintenanceScheduler(integrity, crawler, limiter, 50, 1);

        when(limiter.getPerTwoMinuteLimit()).thenReturn(85);
        when(limiter.remainingTwoMinuteCapacity()).thenReturn(85, 43, 9);
        when(integrity.check()).thenReturn(report(50), report(8));
        when(integrity.repairMissingTimelines(42)).thenReturn(report(8));
        when(integrity.repairMissingTimelines(8)).thenReturn(report(0));
        when(crawler.crawlNextPlayerEUW(20)).thenReturn(Optional.of(
                new CrawlResultDto("EUW1", null, "puuid", 20, 0, java.util.List.of())
        ));

        scheduler.runMaintenanceCycle();

        verify(integrity).repairMissingTimelines(42);
        verify(crawler).crawlNextPlayerEUW(20);
        verify(integrity).repairMissingTimelines(8);
    }

    @Test
    void integrityFailureDoesNotPreventCrawler() {
        DataIntegrityService integrity = mock(DataIntegrityService.class);
        CrawlerService crawler = mock(CrawlerService.class);
        RiotRateLimiter limiter = mock(RiotRateLimiter.class);
        BackgroundMaintenanceScheduler scheduler =
                new BackgroundMaintenanceScheduler(integrity, crawler, limiter, 50, 1);

        when(limiter.getPerTwoMinuteLimit()).thenReturn(85);
        when(limiter.remainingTwoMinuteCapacity()).thenReturn(85, 85, 1);
        when(integrity.check()).
                thenThrow(new IllegalStateException("integrity failed")).
                thenReturn(report(0));

        scheduler.runMaintenanceCycle();

        verify(crawler).crawlNextPlayerEUW(40);
    }

    @Test
    void crawlerFailureDoesNotPreventBorrowedIntegrityPass() {
        DataIntegrityService integrity = mock(DataIntegrityService.class);
        CrawlerService crawler = mock(CrawlerService.class);
        RiotRateLimiter limiter = mock(RiotRateLimiter.class);
        BackgroundMaintenanceScheduler scheduler =
                new BackgroundMaintenanceScheduler(integrity, crawler, limiter, 50, 1);

        when(limiter.getPerTwoMinuteLimit()).thenReturn(85);
        when(limiter.remainingTwoMinuteCapacity()).thenReturn(85, 43, 9);
        when(integrity.check()).thenReturn(report(50), report(8));
        when(integrity.repairMissingTimelines(42)).thenReturn(report(8));
        when(integrity.repairMissingTimelines(8)).thenReturn(report(0));
        when(crawler.crawlNextPlayerEUW(20)).
                thenThrow(new IllegalStateException("crawler failed"));

        scheduler.runMaintenanceCycle();

        verify(integrity).repairMissingTimelines(42);
        verify(integrity).repairMissingTimelines(8);
    }

    @Test
    void disabledPropertyPreventsSchedulerBeanCreation() {
        new ApplicationContextRunner().
                withBean(DataIntegrityService.class, () -> mock(DataIntegrityService.class)).
                withBean(CrawlerService.class, () -> mock(CrawlerService.class)).
                withBean(RiotRateLimiter.class, () -> mock(RiotRateLimiter.class)).
                withUserConfiguration(BackgroundMaintenanceScheduler.class).
                withPropertyValues("app.scheduler.background-maintenance.enabled=false").
                run(context -> assertThat(context).
                        doesNotHaveBean(BackgroundMaintenanceScheduler.class));
    }

    private static DataIntegrityReportDto report(long missingTimelines) {
        boolean valid = missingTimelines == 0;
        return new DataIntegrityReportDto(
                100,
                100 - missingTimelines,
                0,
                0,
                missingTimelines,
                0,
                0,
                valid
        );
    }
}
