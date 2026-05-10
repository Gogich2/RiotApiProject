package org.main.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface TimelineIngestService {

    void ingestTimelineIfMissing(String matchId);

    void ingestTimeline(String matchId, JsonNode timelineJson);

    void repairTimelineFromRaw(String matchId);
}