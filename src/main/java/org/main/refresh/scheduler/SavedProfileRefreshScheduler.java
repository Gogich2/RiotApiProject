package org.main.refresh.scheduler;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.main.account.repository.SavedProfileRepository;
import org.main.refresh.entity.RefreshSource;
import org.main.refresh.service.PlayerRefreshCoordinator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.scheduler.saved-profile-refresh.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SavedProfileRefreshScheduler {

    private final SavedProfileRepository savedProfileRepository;

    private final PlayerRefreshCoordinator coordinator;

    private final Clock clock;

    private final Duration activeWindow;

    private final Duration minimumAge;

    private final int batchSize;

    public SavedProfileRefreshScheduler(
            SavedProfileRepository savedProfileRepository,
            PlayerRefreshCoordinator coordinator,
            Clock clock,
            @Value("${app.refresh.saved-profile-active-window:14d}") Duration activeWindow,
            @Value("${app.refresh.scheduled-min-age:6h}") Duration minimumAge,
            @Value("${app.refresh.scheduled-batch-size:5}") int batchSize
    ) {
        this.savedProfileRepository = savedProfileRepository;
        this.coordinator = coordinator;
        this.clock = clock;
        this.activeWindow = activeWindow;
        this.minimumAge = minimumAge;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.refresh.scheduler-delay:PT15M}")
    public void refreshEligibleProfiles() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        savedProfileRepository.findEligibleForScheduledRefresh(
                now.minus(activeWindow),
                now.minus(minimumAge),
                batchSize
        ).forEach(puuid -> coordinator.enqueue(puuid, RefreshSource.SCHEDULED));
    }
}
