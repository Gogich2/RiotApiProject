package org.main.builds.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.main.builds.BuildProperties;
import org.main.builds.model.BuildChoice;
import org.main.builds.model.BuildConfidence;
import org.main.builds.model.BuildQueue;
import org.main.builds.model.BuildRole;
import org.main.builds.model.BuildSnapshotPayload;
import org.main.builds.model.PatchVersion;
import org.main.builds.store.BuildSnapshot;
import org.main.builds.store.BuildSnapshotRepository;
import org.main.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ChampionBuildService {

    private final BuildSnapshotRepository snapshots;

    private final BuildAssetRepository assets;

    private final BuildProperties properties;

    public ChampionBuildService(
            BuildSnapshotRepository snapshots,
            BuildAssetRepository assets,
            BuildProperties properties
    ) {
        this.snapshots = snapshots;
        this.assets = assets;
        this.properties = properties;
    }

    public ChampionBuildOptionsResponse options(
            int championId, Integer queueId, String patch, BuildRole role) {
        BuildQueue requestedQueue = queueId == null ? null : BuildQueue.fromId(queueId);
        String requestedPatch = patch == null ? null : PatchVersion.parse(patch).displayName();
        requireChampion(championId);
        Map<BuildQueue, List<BuildSnapshot>> byQueue = new EnumMap<>(BuildQueue.class);
        for (BuildQueue queue : BuildQueue.values()) {
            byQueue.put(queue, snapshots.findPublishedForChampion(
                    properties.aggregationVersion(), queue, championId));
        }
        Map<BuildQueue, Boolean> availability = new EnumMap<>(BuildQueue.class);
        byQueue.forEach((queue, values) -> availability.put(
                queue, available(values, requestedPatch, role)));
        BuildQueue selectedQueue = requestedQueue == null
                ? defaultQueue(availability) : requestedQueue;
        List<BuildSnapshot> selected = byQueue.get(selectedQueue);
        List<BuildSnapshot> patchCandidates = selected.isEmpty()
                ? byQueue.values().stream().flatMap(List::stream).toList()
                : selected;
        List<PatchOption> patches = patchCandidates.stream().
                map(snapshot -> snapshot.window().anchorPatch()).
                distinct().
                sorted(Comparator.comparing(PatchVersion::parse).reversed()).
                map(PatchOption::new).
                toList();
        String selectedPatch = requestedPatch != null ? requestedPatch
                : patches.stream().findFirst().map(PatchOption::patch).orElse(null);
        Map<BuildRole, Integer> roleGames = new EnumMap<>(BuildRole.class);
        selected.stream().filter(snapshot -> snapshot.opponentChampionId() == null).
                filter(snapshot -> selectedPatch == null
                        || selectedPatch.equals(snapshot.window().anchorPatch())).
                forEach(snapshot -> roleGames.merge(
                        snapshot.role(), snapshot.games(), Math::max));
        BuildRole selectedRole = role == null
                ? roleGames.entrySet().stream().
                        max(Map.Entry.comparingByValue()).
                        map(Map.Entry::getKey).orElse(BuildRole.TOP)
                : role;
        List<RoleOption> roles = java.util.Arrays.stream(BuildRole.values()).
                map(candidate -> new RoleOption(
                        candidate, roleGames.getOrDefault(candidate, 0),
                        roleGames.containsKey(candidate))).
                toList();
        List<BuildSnapshot> exact = selected.stream().
                filter(snapshot -> snapshot.opponentChampionId() != null).
                filter(snapshot -> selectedPatch != null
                        && selectedPatch.equals(snapshot.window().anchorPatch())).
                filter(snapshot -> snapshot.role() == selectedRole).
                toList();
        List<Integer> opponentIds = exact.stream().
                map(BuildSnapshot::opponentChampionId).distinct().toList();
        Map<Integer, DisplayAsset> opponents = assets.findChampions(opponentIds);
        List<OpponentOption> opponentOptions = exact.stream().
                collect(java.util.stream.Collectors.toMap(
                        BuildSnapshot::opponentChampionId,
                        Function.identity(),
                        (left, right) -> left.games() >= right.games() ? left : right,
                        LinkedHashMap::new)).values().stream().
                map(snapshot -> opponent(snapshot, opponents)).
                sorted(Comparator.comparing(OpponentOption::label)).
                toList();
        List<QueueOption> queues = List.of(
                queueOption(BuildQueue.SOLO_DUO, "Ranked Solo/Duo", availability),
                queueOption(BuildQueue.FLEX, "Ranked Flex", availability));
        return new ChampionBuildOptionsResponse(championId, queues, patches, roles,
                opponentOptions, new RequestedFilters(
                selectedQueue.id(), selectedPatch, selectedRole, null));
    }

    public ChampionBuildResponse builds(
            int championId,
            int queueId,
            String patch,
            BuildRole role,
            Integer opponentId
    ) {
        BuildQueue queue = BuildQueue.fromId(queueId);
        String anchorPatch = PatchVersion.parse(patch).displayName();
        if (role == null) {
            throw new IllegalArgumentException("Build role is required");
        }
        requireChampion(championId);
        if (opponentId != null) {
            requireChampion(opponentId);
        }
        RequestedFilters requested = new RequestedFilters(
                queue.id(), anchorPatch, role, opponentId);

        Optional<BuildSnapshot> exact = opponentId == null ? Optional.empty()
                : snapshots.findPublished(properties.aggregationVersion(), anchorPatch,
                        queue, championId, role, opponentId).
                        filter(value -> value.games() >= properties.matchupMinGames());
        if (exact.isPresent()) {
            return available(exact.get(), requested, BuildFallbackReason.NONE, false);
        }

        Optional<BuildSnapshot> baseline = snapshots.findPublished(
                properties.aggregationVersion(), anchorPatch,
                queue, championId, role, null);
        if (baseline.isPresent()) {
            BuildFallbackReason reason = opponentId == null
                    ? BuildFallbackReason.NONE
                    : BuildFallbackReason.MATCHUP_SAMPLE_TOO_SMALL;
            return available(baseline.get(), requested, reason, false);
        }

        List<BuildSnapshot> historical = snapshots.findHistoricalBaselines(
                properties.aggregationVersion(), anchorPatch,
                queue, championId, role, properties.historicalLookbackPatches());
        Optional<BuildSnapshot> recentHistorical = historical.stream().
                filter(snapshot -> withinHistoricalLookback(
                        anchorPatch, snapshot.window().anchorPatch())).
                max(Comparator.comparing(
                        snapshot -> PatchVersion.parse(snapshot.window().anchorPatch())));
        if (recentHistorical.isPresent()) {
            return available(recentHistorical.get(), requested,
                    BuildFallbackReason.REQUESTED_PATCH_UNAVAILABLE, true);
        }
        return unavailable(requested);
    }

    private ChampionBuildResponse available(
            BuildSnapshot snapshot,
            RequestedFilters requested,
            BuildFallbackReason reason,
            boolean historical
    ) {
        boolean stale = !historical && snapshots.findLatestRun(
                properties.aggregationVersion(), snapshot.window(), snapshot.queue()).
                filter(run -> "FAILED".equals(run.state())).
                filter(run -> run.completedAt() != null
                        && run.completedAt().isAfter(snapshot.publishedAt())).
                isPresent();
        BuildFallbackReason effectiveReason = stale
                ? BuildFallbackReason.AGGREGATION_FAILED_USING_LAST_PUBLISHED : reason;
        return new ChampionBuildResponse(
                true,
                requested,
                new ResolvedFilters(snapshot.queue().id(),
                        snapshot.window().anchorPatch(), snapshot.window().comparisonPatch(),
                        snapshot.role(), snapshot.opponentChampionId()),
                snapshot.scope(),
                snapshot.confidence(),
                snapshot.games(),
                snapshot.wins(),
                winRate(snapshot.wins(), snapshot.games()),
                stale,
                historical,
                effectiveReason,
                evidence(snapshot),
                explanation(effectiveReason),
                snapshot.calculatedAt(),
                snapshot.publishedAt(),
                enrich(snapshot.payload()));
    }

    private ChampionBuildResponse unavailable(RequestedFilters requested) {
        return new ChampionBuildResponse(false, requested, null, null,
                BuildConfidence.INSUFFICIENT, 0, 0, 0.0, false, false,
                BuildFallbackReason.DATA_UNAVAILABLE, "No published games",
                explanation(BuildFallbackReason.DATA_UNAVAILABLE), null, null,
                DisplayBuildPayload.empty());
    }

    private DisplayBuildPayload enrich(BuildSnapshotPayload payload) {
        List<Integer> itemIds = java.util.stream.Stream.of(
                        payload.startingItems(), payload.boots(),
                        payload.coreItems(), payload.situationalItems()).
                flatMap(List::stream).
                flatMap(choice -> choice.ids().stream()).
                distinct().toList();
        Map<Integer, DisplayAsset> itemAssets = assets.findItems(itemIds);
        Map<Integer, DisplayAsset> runeAssets = assets.findRunes(ids(payload.runePages()));
        Map<Integer, DisplayAsset> spellAssets = assets.findSpells(ids(payload.spellPairs()));
        return new DisplayBuildPayload(
                enrich(payload.startingItems(), itemAssets),
                enrich(payload.boots(), itemAssets),
                enrich(payload.coreItems(), itemAssets),
                enrich(payload.situationalItems(), itemAssets),
                enrich(payload.runePages(), runeAssets),
                enrich(payload.spellPairs(), spellAssets),
                enrich(payload.skillOrders(), skillAssets(ids(payload.skillOrders()))),
                payload.skillMaxPriority());
    }

    private List<DisplayBuildChoice> enrich(
            List<BuildChoice> choices,
            Map<Integer, DisplayAsset> found
    ) {
        List<DisplayBuildChoice> result = new ArrayList<>();
        for (BuildChoice choice : choices) {
            List<DisplayAsset> displayAssets = choice.ids().stream().
                    map(id -> found.getOrDefault(id,
                            new DisplayAsset(id, String.valueOf(id), null))).toList();
            result.add(new DisplayBuildChoice(displayAssets, choice.games(), choice.wins(),
                    percentRate(choice.pickRate()), percentRate(choice.winRate())));
        }
        return List.copyOf(result);
    }

    private List<Integer> ids(List<BuildChoice> choices) {
        return choices.stream().flatMap(choice -> choice.ids().stream()).
                distinct().toList();
    }

    private Map<Integer, DisplayAsset> skillAssets(List<Integer> ids) {
        String[] labels = {"", "Q", "W", "E", "R"};
        Map<Integer, DisplayAsset> result = new LinkedHashMap<>();
        ids.stream().distinct().forEach(id -> result.put(id, new DisplayAsset(
                id, id > 0 && id < labels.length ? labels[id] : String.valueOf(id), null)));
        return result;
    }

    private void requireChampion(int championId) {
        if (assets.findChampion(championId).isEmpty()) {
            throw new NotFoundException("Champion not found: " + championId);
        }
    }

    private BuildQueue defaultQueue(Map<BuildQueue, Boolean> availability) {
        return !availability.get(BuildQueue.SOLO_DUO)
                && availability.get(BuildQueue.FLEX)
                ? BuildQueue.FLEX : BuildQueue.SOLO_DUO;
    }

    private boolean available(
            List<BuildSnapshot> candidates, String patch, BuildRole role) {
        return candidates.stream().
                filter(snapshot -> snapshot.opponentChampionId() == null).
                anyMatch(snapshot -> (patch == null
                        || patch.equals(snapshot.window().anchorPatch()))
                        && (role == null || role == snapshot.role()));
    }

    private QueueOption queueOption(
            BuildQueue queue,
            String label,
            Map<BuildQueue, Boolean> availability
    ) {
        return new QueueOption(queue.id(), label, availability.get(queue));
    }

    private OpponentOption opponent(
            BuildSnapshot snapshot, Map<Integer, DisplayAsset> opponents) {
        int id = snapshot.opponentChampionId();
        DisplayAsset asset = opponents.getOrDefault(
                id, new DisplayAsset(id, String.valueOf(id), null));
        return new OpponentOption(id, asset.label(), asset.imageUrl(), snapshot.games());
    }

    private String evidence(BuildSnapshot snapshot) {
        return snapshot.games() + " games | " + snapshot.wins() + " wins | "
                + snapshot.confidence().name().toLowerCase() + " confidence";
    }

    private double winRate(int wins, int games) {
        return games == 0 ? 0.0 : percentRate((double) wins / games);
    }

    private double percentRate(double rate) {
        return Math.round(rate * 10000.0) / 100.0;
    }

    private boolean withinHistoricalLookback(String requestedPatch, String candidatePatch) {
        PatchVersion requested = PatchVersion.parse(requestedPatch);
        PatchVersion candidate = PatchVersion.parse(candidatePatch);
        int distance = requested.minor() - candidate.minor();
        return requested.major() == candidate.major()
                && distance > 0
                && distance <= properties.historicalLookbackPatches();
    }

    private String explanation(BuildFallbackReason reason) {
        return switch (reason) {
            case NONE -> "Published build evidence matches the requested filters.";
            case MATCHUP_SAMPLE_TOO_SMALL ->
                    "The matchup sample is too small; showing the same-role baseline.";
            case REQUESTED_PATCH_UNAVAILABLE ->
                    "The requested patch is unavailable; showing recent historical evidence.";
            case AGGREGATION_FAILED_USING_LAST_PUBLISHED ->
                    "The latest aggregation failed; showing the last published build.";
            case DATA_UNAVAILABLE ->
                    "No published build is available for this champion, queue, patch, and role.";
        };
    }
}
