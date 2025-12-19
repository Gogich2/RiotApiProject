package org.main.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.main.client.RiotApiClient;
import org.main.dto.CrawlResultDto;
import org.main.persistence.entity.MatchEntity;
import org.main.persistence.repository.MatchRepository;
import org.main.util.SummonerNameNormalizer;
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
    public CrawlResultDto crawlSummonerEUW(String summonerNameRaw, int limitRaw) {
        String summonerName = SummonerNameNormalizer.normalize(summonerNameRaw);
        int limit = clampLimit(limitRaw);

        JsonNode summonerJson = riotApiClient.getSummonerByNameEUW(summonerName);
        if (summonerJson == null || summonerJson.get("puuid") == null) {
            throw new IllegalStateException("Summoner not found or invalid response");
        }
        String puuid = summonerJson.get("puuid").asText();

        // pagination
        int start = 0;
        int pageSize = 20; // Riot allows up to 100, але 20 ок для MVP
        List<String> fetched = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();

        while (fetched.size() < limit) {
            int count = Math.min(pageSize, limit - fetched.size());
            List<String> page = riotApiClient.getMatchIdsByPuuidEurope(puuid, start, count);
            if (page.isEmpty()) break;

            for (String id : page) {
                if (seen.add(id)) {
                    fetched.add(id);
                }
            }
            start += count;
        }

        // save only new (no duplicates)
        List<String> saved = new ArrayList<>();
        for (String matchId : fetched) {
            boolean exists = matchRepository.existsById(matchId);
            if (exists) continue;

            JsonNode matchJson = riotApiClient.getMatchByIdEurope(matchId);
            MatchEntity entity = new MatchEntity();
            entity.setMatchId(matchId);
            entity.setRegion("europe");
            entity.setPlatform("EUW1");
            entity.setRawMatchJson(matchJson == null ? null : matchJson.toString());
            entity.setFetchedAt(OffsetDateTime.now());

            matchRepository.save(entity);
            saved.add(matchId);
        }

        return new CrawlResultDto("EUW1", summonerName, puuid, limit, saved.size(), saved);
    }

    // нетривіальна логіка для тестів пізніше
    static int clampLimit(int limit) {
        if (limit <= 0) return 20;
        if (limit > 100) return 100;
        return limit;
    }
}
