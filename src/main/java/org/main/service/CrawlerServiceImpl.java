package org.main.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.main.client.RiotApiClient;
import org.main.dto.CrawlResultDto;
import org.main.exception.ExternalServiceException;
import org.main.exception.NotFoundException;
import org.main.persistence.entity.MatchEntity;
import org.main.persistence.entity.PlatformShard;
import org.main.persistence.entity.PlayerEntity;
import org.main.persistence.entity.RegionRoute;
import org.main.persistence.repository.MatchRepository;
import org.main.persistence.repository.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrawlerServiceImpl implements CrawlerService {

    private static final Logger log = LoggerFactory.getLogger(CrawlerServiceImpl.class);

    private final RiotApiClient riotApiClient;

    private final MatchRepository matchRepository;

    private final PlayerRepository playerRepository;

    private final TimelineIngestService timelineIngestService;

    public CrawlerServiceImpl(RiotApiClient riotApiClient,
                              MatchRepository matchRepository) {
        this.riotApiClient = riotApiClient;
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
        this.timelineIngestService = timelineIngestService;
    }

    @Override
    @Transactional
    public CrawlResultDto crawlPuuidEUW(String puuidRaw, int limitRaw) {
        String puuid = puuidRaw == null ? "" : puuidRaw.trim();

        if (puuid.isEmpty()) {
            throw new IllegalArgumentException("PUUID is required");
        }

        int limit = clampLimit(limitRaw);

        log.info("Starting crawlPuuidEUW: puuid='{}', requestedLimit={}, effectiveLimit={}",
                puuid, limitRaw, limit);

        List<String> fetchedMatchIds = fetchMatchIds(puuid, limit);
        List<String> savedMatchIds = saveNewMatchesAndTimelines(fetchedMatchIds);

        log.info("crawlPuuidEUW finished: puuid='{}', requestedLimit={}, fetchedUnique={}, savedNewMatches={}",
                puuid, limit, fetchedMatchIds.size(), savedMatchIds.size());

        return new CrawlResultDto(
                "EUW1",
                null,
                puuid,
                limit,
                savedMatchIds.size(),
                savedMatchIds
        );
    }

    @Override
    @Transactional
    public CrawlResultDto crawlRiotIdEUW(String gameNameRaw, String tagLineRaw, int limitRaw) {
        String gameName = gameNameRaw == null ? "" : gameNameRaw.trim();
        String tagLine = tagLineRaw == null ? "" : tagLineRaw.trim();

        if (gameName.isEmpty() || tagLine.isEmpty()) {
            throw new IllegalArgumentException("RiotID is required: gameName and tagLine");
        }

        log.info("Starting crawlRiotIdEUW: gameName='{}', tagLine='{}', requestedLimit={}",
                gameName, tagLine, limitRaw);

        JsonNode accountJson = riotApiClient.getAccountByRiotIdEurope(gameName, tagLine);

        if (accountJson == null || accountJson.get("puuid") == null) {
            throw new NotFoundException("Account not found for Riot ID: " + gameName + "#" + tagLine);
        }

        String puuid = accountJson.get("puuid").asText();

        saveOrUpdatePlayer(puuid, gameName, tagLine);

        int limit = clampLimit(limitRaw);
        List<String> fetchedMatchIds = fetchMatchIds(puuid, limit);
        List<String> savedMatchIds = saveNewMatchesAndTimelines(fetchedMatchIds);

        log.info("crawlRiotIdEUW finished: summoner='{}#{}', puuid='{}', savedNewMatches={}",
                gameName, tagLine, puuid, savedMatchIds.size());

        return new CrawlResultDto(
                "EUW1",
                gameName + "#" + tagLine,
                puuid,
                limit,
                savedMatchIds.size(),
                savedMatchIds
        );
    }

    @Override
    public void crawlSummonerEUW(String acoomer, int limit) {
        log.warn("crawlSummonerEUW is not implemented yet: acoomer='{}', limit={}", acoomer, limit);
        throw new UnsupportedOperationException("crawlSummonerEUW is not implemented yet");
    }

    private void saveOrUpdatePlayer(String puuid, String gameName, String tagLine) {
        OffsetDateTime now = OffsetDateTime.now();

        PlayerEntity player = playerRepository.findById(puuid).
                orElseGet(PlayerEntity::new);

        if (player.getPuuid() == null) {
            player.setPuuid(puuid);
            player.setCreatedAt(now);
        }

        player.setGameName(gameName);
        player.setTagLine(tagLine);
        player.setUpdatedAt(now);

        playerRepository.save(player);

        log.info("Player saved or updated: gameName='{}', tagLine='{}', puuid='{}'",
                gameName, tagLine, puuid);
    }

    private List<String> fetchMatchIds(String puuid, int limit) {
        int start = 0;
        int pageSize = 20;

        List<String> fetched = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();

        while (fetched.size() < limit) {
            int count = Math.min(pageSize, limit - fetched.size());

            log.debug("Requesting match id page: puuid='{}', start={}, count={}",
                    puuid, start, count);

            List<String> page = riotApiClient.getMatchIdsByPuuidEurope(puuid, start, count);

            if (page.isEmpty()) {
                log.info("No more match ids returned from Riot API: puuid='{}', start={}", puuid, start);
                break;
            }

            for (String matchId : page) {
                if (seen.add(matchId)) {
                    fetched.add(matchId);
                }
            }

            start += count;
        }

        return fetched;
    }

    private List<String> saveNewMatchesAndTimelines(List<String> matchIds) {
        List<String> saved = new ArrayList<>();

        for (String matchId : matchIds) {
            if (!matchRepository.existsById(matchId)) {
                JsonNode matchJson = riotApiClient.getMatchByIdEurope(matchId);

                if (matchJson == null) {
                    throw new ExternalServiceException("Riot API returned empty match details for matchId=" + matchId);
                }

                MatchEntity entity = new MatchEntity();
                entity.setMatchId(matchId);
                entity.setRegion(RegionRoute.europe);
                entity.setPlatform(PlatformShard.EUW1);
                entity.setRawMatchJson(matchJson.toString());
                entity.setFetchedAt(OffsetDateTime.now());

                matchRepository.save(entity);
                saved.add(matchId);

                log.info("Saved new match: matchId='{}'", matchId);
            } else {
                log.info("Match already exists, checking timeline: matchId='{}'", matchId);
            }

            timelineIngestService.ingestTimelineIfMissing(matchId);
        }

        return saved;
    }

    static int clampLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }

        if (limit > 100) {
            return 100;
        }

        return limit;
    }
}