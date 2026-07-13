package org.main.builds.aggregate;

import org.main.builds.model.BuildQueue;

public interface ChampionBuildAggregationService {

    AggregationOutcome refresh(BuildQueue queue);
}
