package org.main.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.main.client.RiotApiClient;
import org.main.exception.ExternalServiceException;
import org.main.persistence.entity.MatchTimelineEventEntity;
import org.main.persistence.entity.MatchTimelineFrameEntity;
import org.main.persistence.entity.MatchTimelineRawEntity;
import org.main.persistence.repository.MatchTimelineEventRepository;
import org.main.persistence.repository.MatchTimelineFrameRepository;
import org.main.persistence.repository.MatchTimelineRawRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TimelineIngestServiceImpl implements TimelineIngestService {

    private static final Logger log = LoggerFactory.getLogger(TimelineIngestServiceImpl.class);

    private final RiotApiClient riotApiClient;

    private final MatchTimelineRawRepository timelineRawRepository;

    private final MatchTimelineFrameRepository timelineFrameRepository;

    private final MatchTimelineEventRepository timelineEventRepository;

    private final ObjectMapper objectMapper;

    private final IngestLogService ingestLogService;

    public TimelineIngestServiceImpl(RiotApiClient riotApiClient,
                                     MatchTimelineRawRepository timelineRawRepository,
                                     MatchTimelineFrameRepository timelineFrameRepository,
                                     MatchTimelineEventRepository timelineEventRepository,
                                     ObjectMapper objectMapper,
                                     IngestLogService ingestLogService) {
        this.riotApiClient = riotApiClient;
        this.timelineRawRepository = timelineRawRepository;
        this.timelineFrameRepository = timelineFrameRepository;
        this.timelineEventRepository = timelineEventRepository;
        this.objectMapper = objectMapper;
        this.ingestLogService = ingestLogService;
    }

    @Override
    @Transactional
    public void ingestTimelineIfMissing(String matchId) {
        if (timelineRawRepository.existsById(matchId)) {
            repairOrSkipExistingTimeline(matchId);
            return;
        }

        try {
            JsonNode timelineJson = riotApiClient.getMatchTimelineByIdEurope(matchId);

            if (timelineJson == null) {
                ingestLogService.failed("TIMELINE", matchId, "Riot API returned empty timeline");
                throw new ExternalServiceException("Riot API returned empty timeline for matchId=" + matchId);
            }

            ingestTimeline(matchId, timelineJson);
        } catch (Exception ex) {
            ingestLogService.failed("TIMELINE", matchId, ex.getMessage());
            throw ex;
        }
    }

    @Override
    @Transactional
    public void ingestTimeline(String matchId, JsonNode timelineJson) {
        if (matchId == null || matchId.isBlank()) {
            throw new IllegalArgumentException("matchId is required");
        }

        if (timelineJson == null) {
            throw new IllegalArgumentException("timelineJson is required");
        }

        saveRawTimeline(matchId, timelineJson);
        saveFramesAndEvents(matchId, timelineJson);

        ingestLogService.success("TIMELINE", matchId, "Timeline raw, frames and events ingested");
        log.info("Timeline ingested: matchId='{}'", matchId);
    }

    @Override
    @Transactional
    public void repairTimelineFromRaw(String matchId) {
        MatchTimelineRawEntity rawEntity = timelineRawRepository.findById(matchId).
                orElseThrow(() -> new IllegalStateException("Timeline raw not found: " + matchId));

        try {
            JsonNode timelineJson = objectMapper.readTree(rawEntity.getRawTimelineJson());
            saveFramesAndEvents(matchId, timelineJson);

            ingestLogService.success("TIMELINE_REPAIR", matchId, "Timeline repaired from raw JSON");
            log.info("Timeline repaired from raw JSON: matchId='{}'", matchId);
        } catch (Exception ex) {
            ingestLogService.failed("TIMELINE_REPAIR", matchId, ex.getMessage());
            throw new IllegalStateException("Cannot repair timeline from raw JSON for matchId=" + matchId, ex);
        }
    }

    private void repairOrSkipExistingTimeline(String matchId) {
        boolean hasFrames = timelineFrameRepository.countByMatchId(matchId) > 0;
        boolean hasEvents = timelineEventRepository.countByMatchId(matchId) > 0;

        if (hasFrames && hasEvents) {
            ingestLogService.skipped("TIMELINE", matchId, "Timeline already complete");
            log.info("Timeline already complete, skipping Riot API call: matchId='{}'", matchId);
            return;
        }

        ingestLogService.skipped("TIMELINE_RAW", matchId, "Timeline raw exists, parsed data incomplete");

        log.warn(
                "Timeline raw exists, but parsed data is incomplete. Repairing from raw JSON: matchId='{}'",
                matchId
        );

        repairTimelineFromRaw(matchId);
    }

    private void saveRawTimeline(String matchId, JsonNode timelineJson) {
        if (timelineRawRepository.existsById(matchId)) {
            return;
        }

        MatchTimelineRawEntity rawEntity = new MatchTimelineRawEntity();
        rawEntity.setMatchId(matchId);
        rawEntity.setRawTimelineJson(timelineJson.toString());
        rawEntity.setFetchedAt(OffsetDateTime.now());

        timelineRawRepository.save(rawEntity);
    }

    private void saveFramesAndEvents(String matchId, JsonNode timelineJson) {
        JsonNode frames = timelineJson.path("info").path("frames");

        if (!frames.isArray()) {
            log.warn("Timeline has no frames array: matchId='{}'", matchId);
            return;
        }

        timelineEventRepository.deleteByMatchId(matchId);
        timelineEventRepository.flush();

        timelineFrameRepository.deleteByMatchId(matchId);
        timelineFrameRepository.flush();

        int frameNo = 0;

        for (JsonNode frame : frames) {
            saveFrame(matchId, frameNo, frame);
            saveFrameEvents(matchId, frameNo, frame);
            frameNo++;
        }
    }

    private void saveFrame(String matchId, int frameNo, JsonNode frame) {
        MatchTimelineFrameEntity entity = new MatchTimelineFrameEntity();

        entity.setMatchId(matchId);
        entity.setFrameNo(frameNo);
        entity.setTimestampMs(nullableLong(frame, "timestamp"));
        entity.setParticipantFrames(jsonOrNull(frame.get("participantFrames")));
        entity.setRawFrameJson(frame.toString());

        timelineFrameRepository.save(entity);
    }

    private void saveFrameEvents(String matchId, int frameNo, JsonNode frame) {
        JsonNode events = frame.path("events");

        if (!events.isArray()) {
            return;
        }

        List<MatchTimelineEventEntity> entities = new ArrayList<>();

        for (JsonNode event : events) {
            MatchTimelineEventEntity entity = buildEventEntity(matchId, frameNo, event);
            entities.add(entity);
        }

        timelineEventRepository.saveAll(entities);
    }

    private MatchTimelineEventEntity buildEventEntity(String matchId, int frameNo, JsonNode event) {
        MatchTimelineEventEntity entity = new MatchTimelineEventEntity();

        entity.setMatchId(matchId);
        entity.setFrameNo(frameNo);
        entity.setTsMs(nullableLong(event, "timestamp"));
        entity.setType(nullableText(event, "type"));

        entity.setParticipantId(nullableShort(event, "participantId"));
        entity.setKillerId(nullableShort(event, "killerId"));
        entity.setVictimId(nullableShort(event, "victimId"));

        entity.setAssistingParticipantIds(jsonOrNull(event.get("assistingParticipantIds")));

        entity.setItemId(nullableInteger(event, "itemId"));
        entity.setSkillSlot(nullableShort(event, "skillSlot"));
        entity.setLevelUpType(nullableText(event, "levelUpType"));
        entity.setWardType(nullableText(event, "wardType"));
        entity.setBuildingType(nullableText(event, "buildingType"));
        entity.setLaneType(nullableText(event, "laneType"));
        entity.setBounty(nullableInteger(event, "bounty"));

        entity.setPosition(jsonOrNull(event.get("position")));
        entity.setOtherJson(event.toString());

        return entity;
    }

    private String jsonOrNull(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }

        return node.toString();
    }

    private String nullableText(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);

        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }

        String text = value.asText();

        return text == null || text.isBlank() ? null : text;
    }

    private Integer nullableInteger(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);

        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }

        return value.asInt();
    }

    private Long nullableLong(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);

        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }

        return value.asLong();
    }

    private Short nullableShort(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);

        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }

        return (short) value.asInt();
    }
}