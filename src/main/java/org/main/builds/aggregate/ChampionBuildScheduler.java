package org.main.builds.aggregate;

import org.main.builds.model.BuildQueue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.builds.scheduler-enabled",
        havingValue = "true"
)
public final class ChampionBuildScheduler {

    private final ChampionBuildAggregationService service;

    public ChampionBuildScheduler(ChampionBuildAggregationService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${app.builds.scheduler-delay:PT1H}")
    public void refreshSoloDuo() {
        service.refresh(BuildQueue.SOLO_DUO);
    }
}
