package org.main.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.main.client.RiotApiClient;
import org.main.dto.CrawlResultDto;
import org.main.persistence.entity.MatchEntity;
import org.main.persistence.entity.PlatformShard;
import org.main.persistence.repository.MatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
public class CrawlerServiceImpl implements CrawlerService {

    private final RiotApiClient riotApiClient;

    private final MatchRepository matchRepository;

    public CrawlerServiceImpl(RiotApiClient riotApiClient, MatchRepository matchRepository) {
        this.riotApiClient = riotApiClient;
        this.matchRepository = matchRepository;
    }

    @Override
    @Transactional
    public CrawlResultDto crawlPuuidEUW(String puuidRaw, int limitRaw) {
        String puuid = puuidRaw == null ? "" : puuidRaw.trim();
        if (puuid.isEmpty()) {
            throw new IllegalArgumentException("PUUID is required");
        }

        int limit = clampLimit(limitRaw);

        // pagination
        int start = 0;
        int pageSize = 20;
        List<String> fetched = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();

        while (fetched.size() < limit) {
            int count = Math.min(pageSize, limit - fetched.size());
            List<String> page = riotApiClient.getMatchIdsByPuuidEurope(puuid, start, count);
            if (page.isEmpty()) {
                break;
            }

            for (String id : page) {
                if (seen.add(id)) {
                    fetched.add(id);
                }
            }
            start += count;
        }

        List<String> saved = new ArrayList<>();
        for (String matchId : fetched) {
            if (matchRepository.existsById(matchId)) {
                continue;
            }

            JsonNode matchJson = riotApiClient.getMatchByIdEurope(matchId);

            MatchEntity entity = new MatchEntity();
            entity.setMatchId(matchId);
            entity.setRegion(org.main.persistence.entity.RegionRoute.europe);
            entity.setPlatform(PlatformShard.EUW1);
            entity.setRawMatchJson(matchJson == null ? null : matchJson.toString());
            entity.setFetchedAt(OffsetDateTime.now());

            matchRepository.save(entity);
            saved.add(matchId);
        }

        return new CrawlResultDto("EUW1", null, puuid, limit, saved.size(), saved);
    }

    @Override
    @Transactional
    public CrawlResultDto crawlRiotIdEUW(String gameNameRaw, String tagLineRaw, int limitRaw) {
        String gameName = gameNameRaw == null ? "" : gameNameRaw.trim();
        String tagLine = tagLineRaw == null ? "" : tagLineRaw.trim();

        if (gameName.isEmpty() || tagLine.isEmpty()) {
            throw new IllegalArgumentException("RiotID is required: gameName and tagLine");
        }

        JsonNode accountJson = riotApiClient.getAccountByRiotIdEurope(gameName, tagLine);
        if (accountJson == null || accountJson.get("puuid") == null) {
            throw new IllegalStateException("Account not found or invalid response");
        }

        String puuid = accountJson.get("puuid").asText();
        CrawlResultDto result = crawlPuuidEUW(puuid, limitRaw);

        return new CrawlResultDto("EUW1", gameName + "#" + tagLine, puuid,
                result.requestedLimit(), result.savedNewMatches(), result.savedMatchIds());
    }

    @Override
    public void crawlSummonerEUW(String acoomer, int limit) {

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
