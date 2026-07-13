package org.main.builds;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.builds")
public record BuildProperties(
        int aggregationVersion,
        int payloadSchemaVersion,
        int matchupMinGames,
        int mediumConfidenceGames,
        int highConfidenceGames,
        double anchorPatchWeight,
        double comparisonPatchWeight,
        int historicalLookbackPatches,
        Duration startingItemsCutoff,
        int batchSize,
        Duration schedulerDelay,
        boolean schedulerEnabled
) {
}
