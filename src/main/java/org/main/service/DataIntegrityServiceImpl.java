package org.main.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.List;
import org.main.client.RiotApiClient;
import org.main.dto.DataIntegrityReportDto;
import org.main.dto.PlayerProfileRepairResultDto;
import org.main.dto.RankRepairResultDto;
import org.main.persistence.entity.PlayerEntity;
import org.main.persistence.repository.MatchRepository;
import org.main.persistence.repository.MatchTimelineEventRepository;
import org.main.persistence.repository.MatchTimelineFrameRepository;
import org.main.persistence.repository.MatchTimelineRawRepository;
import org.main.persistence.repository.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class DataIntegrityServiceImpl implements DataIntegrityService {

    private static final Logger log = LoggerFactory.getLogger(DataIntegrityServiceImpl.class);

    private final MatchRepository matchRepository;

    private final MatchTimelineRawRepository timelineRawRepository;

    private final MatchTimelineFrameRepository timelineFrameRepository;

    private final MatchTimelineEventRepository timelineEventRepository;

    private final TimelineIngestService timelineIngestService;

    private final PlayerRepository playerRepository;

    private final RankEnrichmentService rankEnrichmentService;

    private final RiotApiClient riotApiClient;

    public DataIntegrityServiceImpl(MatchRepository matchRepository,
                                    MatchTimelineRawRepository timelineRawRepository,
                                    MatchTimelineFrameRepository timelineFrameRepository,
                                    MatchTimelineEventRepository timelineEventRepository,
                                    TimelineIngestService timelineIngestService,
                                    PlayerRepository playerRepository,
                                    RankEnrichmentService rankEnrichmentService,
                                    RiotApiClient riotApiClient) {
        this.matchRepository = matchRepository;
        this.timelineRawRepository = timelineRawRepository;
        this.timelineFrameRepository = timelineFrameRepository;
        this.timelineEventRepository = timelineEventRepository;
        this.timelineIngestService = timelineIngestService;
        this.playerRepository = playerRepository;
        this.rankEnrichmentService = rankEnrichmentService;
        this.riotApiClient = riotApiClient;
    }

    @Override
    public DataIntegrityReportDto check() {
        long matchesTotal = matchRepository.count();
        long timelinesRawTotal = timelineRawRepository.count();
        long framesTotal = timelineFrameRepository.count();
        long eventsTotal = timelineEventRepository.count();

        long matchesWithoutTimelineRaw = matchRepository.countMatchesWithoutTimelineRaw();
        long timelinesWithoutFrames = matchRepository.countTimelinesWithoutFrames();
        long timelinesWithoutEvents = matchRepository.countTimelinesWithoutEvents();

        boolean valid = matchesWithoutTimelineRaw == 0
                && timelinesWithoutFrames == 0
                && timelinesWithoutEvents == 0;

        return new DataIntegrityReportDto(
                matchesTotal,
                timelinesRawTotal,
                framesTotal,
                eventsTotal,
                matchesWithoutTimelineRaw,
                timelinesWithoutFrames,
                timelinesWithoutEvents,
                valid
        );
    }

    @Override
    public DataIntegrityReportDto repairMissingTimelines(int limitRaw) {
        int limit = normalizeLimit(limitRaw);

        List<String> matchesWithoutTimeline = matchRepository.findMatchIdsWithoutTimelineRaw().
                stream().
                limit(limit).
                toList();

        for (String matchId : matchesWithoutTimeline) {
            log.info("Repairing missing timeline: matchId='{}'", matchId);
            timelineIngestService.ingestTimelineIfMissing(matchId);
        }

        repairStoredTimelineProjections(limit);

        return check();
    }

    @Override
    public DataIntegrityReportDto repairStoredTimelineData(int limitRaw) {
        int limit = normalizeLimit(limitRaw);

        repairStoredTimelineProjections(limit);

        return check();
    }

    private void repairStoredTimelineProjections(int limit) {
        List<String> timelinesWithoutFrames = matchRepository.findTimelineRawIdsWithoutFrames().
                stream().
                limit(limit).
                toList();

        for (String matchId : timelinesWithoutFrames) {
            log.info("Repairing missing timeline frames: matchId='{}'", matchId);
            timelineIngestService.repairTimelineFromRaw(matchId);
        }

        List<String> timelinesWithoutEvents = matchRepository.findTimelineRawIdsWithoutEvents().
                stream().
                limit(limit).
                toList();

        for (String matchId : timelinesWithoutEvents) {
            log.info("Repairing missing timeline events: matchId='{}'", matchId);
            timelineIngestService.repairTimelineFromRaw(matchId);
        }
    }

    @Override
    public RankRepairResultDto repairMissingRanks(int limitRaw) {
        int limit = normalizeLimit(limitRaw);

        List<PlayerEntity> players = playerRepository.findPlayersMissingRanks(PageRequest.of(0, limit));

        int enriched = 0;
        int changed = 0;

        for (PlayerEntity player : players) {
            try {
                RankEnrichmentResult result = rankEnrichmentService.enrichRanksForPuuidEuw(player.getPuuid());

                if (result.hasEntries()) {
                    enriched++;
                }

                if (result.changed()) {
                    changed++;
                }
            } catch (Exception ex) {
                log.warn("Could not enrich rank: puuid='{}'", player.getPuuid(), ex);
            }
        }

        return new RankRepairResultDto(players.size(), enriched, changed);
    }

    @Override
    public PlayerProfileRepairResultDto repairMissingPlayerProfiles(int limitRaw) {
        int limit = normalizeLimit(limitRaw);

        List<PlayerEntity> players = playerRepository.findPlayersMissingProfiles(PageRequest.of(0, limit));

        int enriched = 0;

        for (PlayerEntity player : players) {
            try {
                JsonNode summoner = riotApiClient.getSummonerByPuuidEuw(player.getPuuid());
                Integer profileIconId = intValue(summoner, "profileIconId");

                if (profileIconId == null) {
                    log.warn("Could not enrich player profile because profileIconId is missing: puuid='{}'",
                            player.getPuuid());
                    continue;
                }

                player.setProfileIconId(profileIconId);
                player.setSummonerLevel(intValue(summoner, "summonerLevel"));
                player.setProfileSyncedAt(OffsetDateTime.now());
                player.setUpdatedAt(OffsetDateTime.now());

                playerRepository.save(player);
                enriched++;

                log.info(
                        "Player profile enriched: puuid='{}', profileIconId={}",
                        player.getPuuid(),
                        profileIconId
                );
            } catch (Exception ex) {
                log.warn("Could not enrich player profile: puuid='{}'", player.getPuuid(), ex);
            }
        }

        return new PlayerProfileRepairResultDto(players.size(), enriched);
    }

    private Integer intValue(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            return null;
        }

        return node.get(field).asInt();
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }

        return Math.min(limit, 500);
    }
}
