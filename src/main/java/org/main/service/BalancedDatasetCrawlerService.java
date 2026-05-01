package org.main.service;

import java.util.List;
import org.main.dto.BalancedDatasetResultDto;

public interface BalancedDatasetCrawlerService {

    BalancedDatasetResultDto collectBalancedDatasetEUW(
            List<String> seedPuuids,
            int targetPerBucket,
            int matchesPerPlayer,
            int maxPlayersToVisit
    );
}