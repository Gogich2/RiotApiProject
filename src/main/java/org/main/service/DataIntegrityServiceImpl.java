package org.main.service;

import java.util.List;
import org.main.dto.DataIntegrityReportDto;
import org.main.dto.RankRepairResultDto;
import org.main.persistence.entity.PlayerEntity;
import org.main.persistence.repository.MatchRepository;
import org.main.persistence.repository.MatchTimelineEventRepository;
import org.main.persistence.repository.MatchTimelineFrameRepository;
import org.main.persistence.repository.MatchTimelineRawRepository;
import org.main.persistence.repository.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public DataIntegrityServiceImpl(MatchRepository matchRepository,
                                    MatchTimelineRawRepository timelineRawRepository,
                                    MatchTimelineFrameRepository timelineFrameRepository,
                                    MatchTimelineEventRepository timelineEventRepository,
                                    TimelineIngestService timelineIngestService,
                                    PlayerRepository playerRepository,
                                    RankEnrichmentService rankEnrichmentService) {
        this.matchRepository = matchRepository;
        this.timelineRawRepository = timelineRawRepository;
        this.timelineFrameRepository = timelineFrameRepository;
        this.timelineEventRepository = timelineEventRepository;
        this.timelineIngestService = timelineIngestService;
        this.playerRepository = playerRepository;
        this.rankEnrichmentService = rankEnrichmentService;
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

        return check();
    }

    @Override
    public RankRepairResultDto repairMissingRanks(int limitRaw) {
        int limit = normalizeLimit(limitRaw);

        List<PlayerEntity> players = playerRepository.findAll().
                stream().
                filter(player -> player.getPuuid() != null && !player.getPuuid().isBlank()).
                filter(player -> !rankEnrichmentService.hasRankData(player.getPuuid())).
                limit(limit).
                toList();

        int enriched = 0;

        for (PlayerEntity player : players) {
            try {
                if (!rankEnrichmentService.enrichRanksForPuuidEuw(player.getPuuid()).isEmpty()) {
                    enriched++;
                }
            } catch (Exception ex) {
                log.warn("Could not enrich rank: puuid='{}'", player.getPuuid(), ex);
            }
        }

        return new RankRepairResultDto(players.size(), enriched);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }

        return Math.min(limit, 500);
    }
}