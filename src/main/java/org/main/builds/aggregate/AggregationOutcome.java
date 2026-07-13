package org.main.builds.aggregate;

import java.util.UUID;

public record AggregationOutcome(
        Status status,
        String patch,
        int sourceMatches,
        int snapshots,
        UUID runId
) {

    public enum Status {
        PUBLISHED,
        NO_CHANGE,
        INSUFFICIENT_SOURCE_DATA,
        FAILED
    }
}
