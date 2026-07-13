package org.main.builds.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record AggregationResult(
        List<AggregatedCohort> cohorts,
        Set<BaselineKey> expectedBaselines,
        int sourceObservationCount
) {

    public AggregationResult {
        cohorts = List.copyOf(cohorts);
        expectedBaselines = Collections.unmodifiableSet(new LinkedHashSet<>(expectedBaselines));
    }
}
