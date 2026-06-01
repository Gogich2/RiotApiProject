package org.main.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.main.client.RiotApiClient;
import org.main.persistence.entity.LeagueEntryEntity;
import org.main.persistence.entity.LeagueEntrySnapshotEntity;
import org.main.persistence.entity.PlatformShard;
import org.main.persistence.repository.LeagueEntryRepository;
import org.main.persistence.repository.LeagueEntrySnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RankEnrichmentServiceImpl implements RankEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(RankEnrichmentServiceImpl.class);

    private static final PlatformShard PLATFORM = PlatformShard.EUW1;

    private final RiotApiClient riotApiClient;

    private final LeagueEntryRepository leagueEntryRepository;

    private final LeagueEntrySnapshotRepository leagueEntrySnapshotRepository;

    public RankEnrichmentServiceImpl(RiotApiClient riotApiClient,
                                     LeagueEntryRepository leagueEntryRepository,
                                     LeagueEntrySnapshotRepository leagueEntrySnapshotRepository) {
        this.riotApiClient = riotApiClient;
        this.leagueEntryRepository = leagueEntryRepository;
        this.leagueEntrySnapshotRepository = leagueEntrySnapshotRepository;
    }

    @Override
    public RankEnrichmentResult enrichRanksForPuuidEuw(String puuid) {
        if (puuid == null || puuid.isBlank()) {
            return new RankEnrichmentResult(List.of(), false);
        }

        OffsetDateTime now = OffsetDateTime.now();

        JsonNode leagueEntries = riotApiClient.getLeagueEntriesByPuuidEuw(puuid);

        if (leagueEntries == null || !leagueEntries.isArray()) {
            return new RankEnrichmentResult(List.of(), false);
        }

        List<LeagueEntryEntity> saved = new ArrayList<>();
        boolean changed = false;

        for (JsonNode entryNode : leagueEntries) {
            String queueType = text(entryNode, "queueType");

            if (queueType == null || queueType.isBlank()) {
                continue;
            }

            LeagueEntryEntity entry = leagueEntryRepository.
                    findByPlatformAndPuuidAndQueueType(PLATFORM, puuid, queueType).
                    orElseGet(LeagueEntryEntity::new);

            boolean entryChanged = isNewEntry(entry) || isChanged(entry, entryNode);

            entry.setPlatform(PLATFORM);
            entry.setSummonerId(text(entryNode, "summonerId"));
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

            LeagueEntryEntity savedEntry = leagueEntryRepository.save(entry);
            saved.add(savedEntry);

            if (entryChanged) {
                leagueEntrySnapshotRepository.save(createSnapshot(savedEntry, now));
                changed = true;
            }
        }

        log.info(
                "Rank enrichment completed: puuid='{}', entries={}, changed={}",
                puuid,
                saved.size(),
                changed
        );

        return new RankEnrichmentResult(saved, changed);
    }

    @Override
    public boolean hasRankData(String puuid) {
        return puuid != null && !puuid.isBlank() && leagueEntryRepository.existsByPuuid(puuid);
    }

    private boolean isNewEntry(LeagueEntryEntity entry) {
        return entry.getId() == null;
    }

    private boolean isChanged(LeagueEntryEntity current, JsonNode incoming) {
        return !Objects.equals(current.getTier(), text(incoming, "tier"))
                || !Objects.equals(current.getRankValue(), text(incoming, "rank"))
                || !Objects.equals(current.getLeaguePoints(), intValue(incoming, "leaguePoints"))
                || !Objects.equals(current.getWins(), intValue(incoming, "wins"))
                || !Objects.equals(current.getLosses(), intValue(incoming, "losses"))
                || !Objects.equals(current.getHotStreak(), booleanValue(incoming, "hotStreak"))
                || !Objects.equals(current.getVeteran(), booleanValue(incoming, "veteran"))
                || !Objects.equals(current.getFreshBlood(), booleanValue(incoming, "freshBlood"))
                || !Objects.equals(current.getInactive(), booleanValue(incoming, "inactive"));
    }

    private LeagueEntrySnapshotEntity createSnapshot(LeagueEntryEntity entry, OffsetDateTime syncedAt) {
        LeagueEntrySnapshotEntity snapshot = new LeagueEntrySnapshotEntity();

        snapshot.setPuuid(entry.getPuuid());
        snapshot.setSummonerId(entry.getSummonerId());
        snapshot.setPlatform(entry.getPlatform());
        snapshot.setQueueType(entry.getQueueType());
        snapshot.setTier(entry.getTier());
        snapshot.setRankValue(entry.getRankValue());
        snapshot.setLeaguePoints(entry.getLeaguePoints());
        snapshot.setWins(entry.getWins());
        snapshot.setLosses(entry.getLosses());
        snapshot.setHotStreak(entry.getHotStreak());
        snapshot.setVeteran(entry.getVeteran());
        snapshot.setFreshBlood(entry.getFreshBlood());
        snapshot.setInactive(entry.getInactive());
        snapshot.setSyncedAt(syncedAt);

        return snapshot;
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

    private Boolean booleanValue(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            return null;
        }

        return node.get(field).asBoolean();
    }
}