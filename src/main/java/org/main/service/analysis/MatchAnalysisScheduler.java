package org.main.service.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.scheduler.match-analysis.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MatchAnalysisScheduler {

    private static final Logger log = LoggerFactory.getLogger(MatchAnalysisScheduler.class);

    private final MatchAnalysisService matchAnalysisService;

    public MatchAnalysisScheduler(MatchAnalysisService matchAnalysisService) {
        this.matchAnalysisService = matchAnalysisService;
    }

    @Scheduled(fixedDelay = 10000)
    public void processNewMatches() {
        int processed = matchAnalysisService.processNewMatches(50);

        if (processed > 0) {
            log.info("Analysis scheduler processed {} matches", processed);
        }
    }
}
