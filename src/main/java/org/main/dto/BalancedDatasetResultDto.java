package org.main.dto;

import java.util.Map;

public record BalancedDatasetResultDto(
        int targetPerBucket,
        int visitedPlayers,
        int scannedMatches,
        int savedNewMatches,
        int skippedMatches,
        boolean balanced,
        Map<String, Integer> bucketCounts
) {
}