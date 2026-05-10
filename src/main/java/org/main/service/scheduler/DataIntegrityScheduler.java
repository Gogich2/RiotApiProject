package org.main.service.scheduler;

import org.main.dto.DataIntegrityReportDto;
import org.main.service.DataIntegrityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.scheduler.data-integrity.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class DataIntegrityScheduler {

    private static final Logger log = LoggerFactory.getLogger(DataIntegrityScheduler.class);

    private static final int REPAIR_LIMIT = 50;

    private final DataIntegrityService dataIntegrityService;

    public DataIntegrityScheduler(DataIntegrityService dataIntegrityService) {
        this.dataIntegrityService = dataIntegrityService;
    }

    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    public void checkAndRepairTimelineData() {
        try {
            DataIntegrityReportDto before = dataIntegrityService.check();

            if (before.valid()) {
                log.debug("Scheduled data integrity check passed");
                return;
            }

            log.warn(
                    "Scheduled data integrity check found issues: matchesWithoutTimelineRaw={}, "
                            + "timelinesWithoutFrames={}, timelinesWithoutEvents={}",
                    before.matchesWithoutTimelineRaw(),
                    before.timelinesWithoutFrames(),
                    before.timelinesWithoutEvents()
            );

            DataIntegrityReportDto after = dataIntegrityService.repairMissingTimelines(REPAIR_LIMIT);

            log.info(
                    "Scheduled data integrity repair finished: valid={}, matchesWithoutTimelineRaw={}, "
                            + "timelinesWithoutFrames={}, timelinesWithoutEvents={}",
                    after.valid(),
                    after.matchesWithoutTimelineRaw(),
                    after.timelinesWithoutFrames(),
                    after.timelinesWithoutEvents()
            );
        } catch (Exception ex) {
            log.error("Scheduled data integrity repair failed", ex);
        }
    }
}