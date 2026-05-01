package org.main.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.main.client.RiotApiClient;
import org.main.dto.BalancedDatasetResultDto;
import org.main.exception.ExternalServiceException;
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
public class BalancedDatasetCrawlerServiceImpl implements BalancedDatasetCrawlerService {

    private static final Logger log = LoggerFactory.getLogger(BalancedDatasetCrawlerServiceImpl.class);

    private static final int RANKED_SOLO_QUEUE_ID = 420;

    private static final int RANKED_FLEX_QUEUE_ID = 440;

    private static final List<String> POSITIONS = List.of(
            "TOP",
            "JUNGLE",
            "MIDDLE",
            "BOTTOM",
            "UTILITY"
    );

    private final RiotApiClient riotApiClient;

    private final MatchRepository matchRepository;

    private final PlayerRepository playerRepository;

    private final ObjectMapper objectMapper;

    private final TimelineIngestService timelineIngestService;

    public BalancedDatasetCrawlerServiceImpl(RiotApiClient riotApiClient,
                                             MatchRepository matchRepository,
                                             PlayerRepository playerRepository,
                                             ObjectMapper objectMapper,
                                             TimelineIngestService timelineIngestService) {
        this.riotApiClient = riotApiClient;
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
        this.objectMapper = objectMapper;
        this.timelineIngestService = timelineIngestService;
    }

    @Override
    @Transactional
    public BalancedDatasetResultDto collectBalancedDatasetEUW(List<String> seedPuuids,
                                                              int targetPerBucket,
                                                              int matchesPerPlayer,
                                                              int maxPlayersToVisit) {
        if (seedPuuids == null || seedPuuids.isEmpty()) {
            throw new IllegalArgumentException("At least one seed PUUID is required");
        }

        int effectiveTarget = normalizeTarget(targetPerBucket);
        int effectiveMatchesPerPlayer = normalizeMatchesPerPlayer(matchesPerPlayer);
        int effectiveMaxPlayers = normalizeMaxPlayers(maxPlayersToVisit);

        Map<String, Integer> bucketCounts = createEmptyBucketCounts();

        Queue<String> playerQueue = new ArrayDeque<>();
        Set<String> queuedPlayers = new HashSet<>();
        Set<String> visitedPlayers = new HashSet<>();
        Set<String> scannedMatchIds = new HashSet<>();

        for (String seedPuuid : seedPuuids) {
            String cleanPuuid = normalize(seedPuuid);

            if (!cleanPuuid.isEmpty() && queuedPlayers.add(cleanPuuid)) {
                playerQueue.add(cleanPuuid);
            }
        }

        int scannedMatches = 0;
        int savedNewMatches = 0;
        int skippedMatches = 0;

        log.info("Balanced dataset crawl started: seeds={}, targetPerBucket={}, matchesPerPlayer={}, maxPlayers={}",
                playerQueue.size(), effectiveTarget, effectiveMatchesPerPlayer, effectiveMaxPlayers);

        while (!playerQueue.isEmpty()
                && visitedPlayers.size() < effectiveMaxPlayers
                && !isBalanced(bucketCounts, effectiveTarget)) {

            String currentPuuid = playerQueue.poll();

            if (currentPuuid == null || !visitedPlayers.add(currentPuuid)) {
                continue;
            }

            log.info("Visiting player for dataset crawl: puuid='{}', visited={}/{}",
                    currentPuuid, visitedPlayers.size(), effectiveMaxPlayers);

            List<String> matchIds = riotApiClient.getMatchIdsByPuuidEurope(
                    currentPuuid,
                    0,
                    effectiveMatchesPerPlayer
            );

            for (String matchId : matchIds) {
                if (isBalanced(bucketCounts, effectiveTarget)) {
                    break;
                }

                if (!scannedMatchIds.add(matchId)) {
                    continue;
                }

                scannedMatches++;

                JsonNode matchJson = loadMatchJson(matchId);

                if (!isUsefulMatch(matchJson)) {
                    skippedMatches++;
                    continue;
                }

                updateBucketCounts(bucketCounts, matchJson, effectiveTarget);
                saveParticipantsAsPlayers(matchJson);
                addParticipantsToQueue(matchJson, playerQueue, queuedPlayers, visitedPlayers);

                if (!matchRepository.existsById(matchId)) {
                    saveMatch(matchId, matchJson);
                    savedNewMatches++;
                }

                timelineIngestService.ingestTimelineIfMissing(matchId);
            }
        }

        boolean balanced = isBalanced(bucketCounts, effectiveTarget);

        log.info("Balanced dataset crawl finished: balanced={},"
                        +
                        " visitedPlayers={}, scannedMatches={}, savedNewMatches={}, skippedMatches={}",
                balanced, visitedPlayers.size(), scannedMatches, savedNewMatches, skippedMatches);

        return new BalancedDatasetResultDto(
                effectiveTarget,
                visitedPlayers.size(),
                scannedMatches,
                savedNewMatches,
                skippedMatches,
                balanced,
                bucketCounts
        );
    }

    private JsonNode loadMatchJson(String matchId) {
        if (matchRepository.existsById(matchId)) {
            return loadExistingMatchJson(matchId);
        }

        JsonNode matchJson = riotApiClient.getMatchByIdEurope(matchId);

        if (matchJson == null) {
            throw new ExternalServiceException("Riot API returned empty match details for matchId=" + matchId);
        }

        return matchJson;
    }

    private JsonNode loadExistingMatchJson(String matchId) {
        MatchEntity existing = matchRepository.findById(matchId).
                orElseThrow(() -> new IllegalStateException("Match disappeared while loading: " + matchId));

        try {
            return objectMapper.readTree(existing.getRawMatchJson());
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot parse saved raw_match_json for matchId=" + matchId, ex);
        }
    }

    private boolean isUsefulMatch(JsonNode matchJson) {
        if (matchJson == null || matchJson.get("info") == null) {
            return false;
        }

        JsonNode info = matchJson.get("info");

        int queueId = info.path("queueId").asInt(-1);
        if (queueId != RANKED_SOLO_QUEUE_ID && queueId != RANKED_FLEX_QUEUE_ID) {
            return false;
        }

        String gameMode = info.path("gameMode").asText("");
        if (!"CLASSIC".equalsIgnoreCase(gameMode)) {
            return false;
        }

        JsonNode participants = info.get("participants");
        if (participants == null || !participants.isArray()) {
            return false;
        }

        Set<String> foundPositions = new HashSet<>();

        for (JsonNode participant : participants) {
            String position = extractPosition(participant);
            boolean hasWin = participant.has("win");

            if (POSITIONS.contains(position) && hasWin) {
                foundPositions.add(position);
            }
        }

        return foundPositions.containsAll(POSITIONS);
    }

    private void updateBucketCounts(Map<String, Integer> bucketCounts,
                                    JsonNode matchJson,
                                    int targetPerBucket) {
        JsonNode participants = matchJson.path("info").path("participants");

        if (!participants.isArray()) {
            return;
        }

        for (JsonNode participant : participants) {
            String position = extractPosition(participant);

            if (!POSITIONS.contains(position)) {
                continue;
            }

            boolean win = participant.path("win").asBoolean(false);
            String bucket = buildBucket(position, win);

            int current = bucketCounts.getOrDefault(bucket, 0);

            if (current < targetPerBucket) {
                bucketCounts.put(bucket, current + 1);
            }
        }
    }

    private void saveParticipantsAsPlayers(JsonNode matchJson) {
        JsonNode participants = matchJson.path("info").path("participants");
        OffsetDateTime now = OffsetDateTime.now();

        if (!participants.isArray()) {
            return;
        }

        for (JsonNode participant : participants) {
            String puuid = participant.path("puuid").asText("");

            if (puuid.isBlank()) {
                continue;
            }

            String gameName = participant.path("riotIdGameName").asText(null);
            String tagLine = participant.path("riotIdTagline").asText(null);

            if (gameName == null || gameName.isBlank()) {
                gameName = participant.path("summonerName").asText(null);
            }

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
        }
    }

    private void addParticipantsToQueue(JsonNode matchJson,
                                        Queue<String> playerQueue,
                                        Set<String> queuedPlayers,
                                        Set<String> visitedPlayers) {
        JsonNode participants = matchJson.path("info").path("participants");

        if (!participants.isArray()) {
            return;
        }

        for (JsonNode participant : participants) {
            String puuid = participant.path("puuid").asText("");

            if (puuid.isBlank()) {
                continue;
            }

            if (visitedPlayers.contains(puuid)) {
                continue;
            }

            if (queuedPlayers.add(puuid)) {
                playerQueue.add(puuid);
            }
        }
    }

    private void saveMatch(String matchId, JsonNode matchJson) {
        MatchEntity entity = new MatchEntity();

        entity.setMatchId(matchId);
        entity.setRegion(RegionRoute.europe);
        entity.setPlatform(PlatformShard.EUW1);
        entity.setRawMatchJson(matchJson.toString());
        entity.setFetchedAt(OffsetDateTime.now());

        matchRepository.save(entity);

        log.info("Saved dataset match: matchId='{}'", matchId);
    }

    private Map<String, Integer> createEmptyBucketCounts() {
        Map<String, Integer> counts = new HashMap<>();

        for (String position : POSITIONS) {
            counts.put(buildBucket(position, true), 0);
            counts.put(buildBucket(position, false), 0);
        }

        return counts;
    }

    private boolean isBalanced(Map<String, Integer> bucketCounts, int targetPerBucket) {
        for (String position : POSITIONS) {
            if (bucketCounts.getOrDefault(buildBucket(position, true), 0) < targetPerBucket) {
                return false;
            }

            if (bucketCounts.getOrDefault(buildBucket(position, false), 0) < targetPerBucket) {
                return false;
            }
        }

        return true;
    }

    private String extractPosition(JsonNode participant) {
        String teamPosition = participant.path("teamPosition").asText("");

        if (POSITIONS.contains(teamPosition)) {
            return teamPosition;
        }

        String individualPosition = participant.path("individualPosition").asText("");

        if (POSITIONS.contains(individualPosition)) {
            return individualPosition;
        }

        return "";
    }

    private String buildBucket(String position, boolean win) {
        return position + "_" + (win ? "WIN" : "LOSS");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private int normalizeTarget(int targetPerBucket) {
        if (targetPerBucket <= 0) {
            return 20;
        }

        return Math.min(targetPerBucket, 500);
    }

    private int normalizeMatchesPerPlayer(int matchesPerPlayer) {
        if (matchesPerPlayer <= 0) {
            return 20;
        }

        return Math.min(matchesPerPlayer, 100);
    }

    private int normalizeMaxPlayers(int maxPlayersToVisit) {
        if (maxPlayersToVisit <= 0) {
            return 50;
        }

        return Math.min(maxPlayersToVisit, 500);
    }
}