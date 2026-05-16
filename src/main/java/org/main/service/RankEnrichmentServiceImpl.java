package org.main.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.main.client.RiotApiClient;
import org.main.persistence.entity.LeagueEntryEntity;
import org.main.persistence.entity.PlatformShard;
import org.main.persistence.entity.SummonerEntity;
import org.main.persistence.repository.LeagueEntryRepository;
import org.main.persistence.repository.SummonerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RankEnrichmentServiceImpl implements RankEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(RankEnrichmentServiceImpl.class);

    private static final PlatformShard PLATFORM = PlatformShard.EUW1;

    private final RiotApiClient riotApiClient;

    private final SummonerRepository summonerRepository;

    private final LeagueEntryRepository leagueEntryRepository;

    public RankEnrichmentServiceImpl(RiotApiClient riotApiClient,
                                     SummonerRepository summonerRepository,
                                     LeagueEntryRepository leagueEntryRepository) {
        this.riotApiClient = riotApiClient;
        this.summonerRepository = summonerRepository;
        this.leagueEntryRepository = leagueEntryRepository;
    }

    @Override
    public List<LeagueEntryEntity> enrichRanksForPuuidEuw(String puuid) {
        if (puuid == null || puuid.isBlank()) {
            return List.of();
        }

        OffsetDateTime now = OffsetDateTime.now();

        SummonerEntity summoner = summonerRepository.findByPlatformAndPuuid(PLATFORM, puuid).
                orElseGet(() -> fetchAndSaveSummoner(puuid, now));

        if (summoner.getSummonerId() == null || summoner.getSummonerId().isBlank()) {
            log.warn("Cannot enrich ranks because summonerId is empty: puuid='{}'", puuid);
            return List.of();
        }

        JsonNode leagueEntries = riotApiClient.getLeagueEntriesBySummonerIdEuw(summoner.getSummonerId());

        if (leagueEntries == null || !leagueEntries.isArray()) {
            return List.of();
        }

        List<LeagueEntryEntity> saved = new ArrayList<>();

        for (JsonNode entryNode : leagueEntries) {
            String queueType = text(entryNode, "queueType");

            if (queueType == null || queueType.isBlank()) {
                continue;
            }

            LeagueEntryEntity entry = leagueEntryRepository.
                    findByPlatformAndSummonerIdAndQueueType(PLATFORM, summoner.getSummonerId(), queueType).
                    orElseGet(LeagueEntryEntity::new);

            entry.setPlatform(PLATFORM);
            entry.setSummonerId(summoner.getSummonerId());
            entry.setPuuid(puuid);
            entry.setQueueType(queueType);
            entry.setTier(text(entryNode, "tier"));
            entry.setRankValue(text(entryNode, "rank"));
            entry.setLeaguePoints(intValue(entryNode, "leaguePoints"));
            entry.setWins(intValue(entryNode, "wins"));
            entry.setLosses(intValue(entryNode, "losses"));
            entry.setHotStreak(booleanValue(entryNode, "hotStreak"));
            entry.setVeteran(booleanValue(entryNode, "veteran"));
            entry.setFreshBlood(booleanValue(entryNode, "freshBlood"));
            entry.setInactive(booleanValue(entryNode, "inactive"));
            entry.setLastSyncedAt(now);

            saved.add(leagueEntryRepository.save(entry));
        }

        log.info("Rank enrichment completed: puuid='{}', entries={}", puuid, saved.size());
        return saved;
    }

    @Override
    public boolean hasRankData(String puuid) {
        return puuid != null && !puuid.isBlank() && leagueEntryRepository.existsByPuuid(puuid);
    }

    private SummonerEntity fetchAndSaveSummoner(String puuid, OffsetDateTime now) {
        JsonNode summonerNode = riotApiClient.getSummonerByPuuidEuw(puuid);

        SummonerEntity summoner = new SummonerEntity();
        summoner.setSummonerId(text(summonerNode, "id"));
        summoner.setPuuid(puuid);
        summoner.setPlatform(PLATFORM);
        summoner.setName(text(summonerNode, "name"));
        summoner.setSummonerLevel(intValue(summonerNode, "summonerLevel"));
        summoner.setProfileIconId(intValue(summonerNode, "profileIconId"));
        summoner.setRevisionDateMs(longValue(summonerNode, "revisionDate"));
        summoner.setLastSyncedAt(now);

        return summonerRepository.save(summoner);
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            return null;
        }

        return node.get(field).asText();
    }

    private Integer intValue(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            return null;
        }

        return node.get(field).asInt();
    }

    private Long longValue(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            return null;
        }

        return node.get(field).asLong();
    }

    private Boolean booleanValue(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            return null;
        }

        return node.get(field).asBoolean();
    }
}