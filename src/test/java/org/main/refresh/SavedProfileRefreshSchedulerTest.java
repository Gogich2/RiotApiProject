package org.main.refresh;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.main.account.repository.SavedProfileRepository;
import org.main.refresh.entity.RefreshSource;
import org.main.refresh.scheduler.SavedProfileRefreshScheduler;
import org.main.refresh.service.PlayerRefreshCoordinator;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SavedProfileRefreshSchedulerTest {

    @Mock
    private SavedProfileRepository savedProfileRepository;

    @Mock
    private PlayerRefreshCoordinator coordinator;

    @Test
    void selectsRecentlyViewedStaleProfilesWithinBatchLimit() {
        Instant instant = Instant.parse("2026-07-13T06:00:00Z");
        OffsetDateTime activeSince = OffsetDateTime.ofInstant(instant.minus(Duration.ofDays(14)), ZoneOffset.UTC);
        OffsetDateTime freshSince = OffsetDateTime.ofInstant(instant.minus(Duration.ofHours(6)), ZoneOffset.UTC);
        when(savedProfileRepository.findEligibleForScheduledRefresh(activeSince, freshSince, 5)).
                thenReturn(List.of("first", "second"));
        SavedProfileRefreshScheduler scheduler = new SavedProfileRefreshScheduler(
                savedProfileRepository,
                coordinator,
                Clock.fixed(instant, ZoneOffset.UTC),
                Duration.ofDays(14),
                Duration.ofHours(6),
                5
        );

        scheduler.refreshEligibleProfiles();

        verify(coordinator).enqueue("first", RefreshSource.SCHEDULED);
        verify(coordinator).enqueue("second", RefreshSource.SCHEDULED);
    }
}
