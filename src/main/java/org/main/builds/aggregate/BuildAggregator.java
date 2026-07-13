package org.main.builds.aggregate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.main.builds.BuildRules;
import org.main.builds.model.AggregatedCohort;
import org.main.builds.model.AggregationResult;
import org.main.builds.model.BaselineKey;
import org.main.builds.model.BuildChoice;
import org.main.builds.model.BuildObservation;
import org.main.builds.model.BuildQueue;
import org.main.builds.model.BuildRole;
import org.main.builds.model.BuildScope;
import org.main.builds.model.BuildSnapshotPayload;
import org.main.builds.model.PatchWindow;
import org.main.builds.model.RunePage;

public final class BuildAggregator {

    private static final Comparator<BaselineKey> BASELINE_ORDER = Comparator.
            comparingInt(BaselineKey::championId).
            thenComparing(BaselineKey::role);

    private static final Comparator<ExactKey> EXACT_ORDER = Comparator.
            comparingInt(ExactKey::championId).
            thenComparing(ExactKey::role).
            thenComparingInt(ExactKey::opponentChampionId);

    private final BuildRules rules;

    public BuildAggregator(BuildRules rules) {
        this.rules = rules;
    }

    public AggregationResult aggregate(
            PatchWindow window,
            BuildQueue queue,
            List<BuildObservation> observations
    ) {
        List<BuildObservation> eligible = observations.stream().
                filter(observation -> observation.queue() == queue).
                filter(observation -> observation.patch().equals(window.anchorPatch())
                        || observation.patch().equals(window.comparisonPatch())).
                toList();

        Map<BaselineKey, List<BuildObservation>> baselines = new HashMap<>();
        Map<ExactKey, List<BuildObservation>> exacts = new HashMap<>();
        for (BuildObservation observation : eligible) {
            baselines.computeIfAbsent(new BaselineKey(
                    observation.championId(), observation.role()), ignored -> new ArrayList<>()).
                    add(observation);
            if (observation.opponentChampionId() != null) {
                exacts.computeIfAbsent(new ExactKey(observation.championId(), observation.role(),
                        observation.opponentChampionId()), ignored -> new ArrayList<>()).
                        add(observation);
            }
        }

        BuildComponentRanker ranker = new BuildComponentRanker(rules, window);
        List<BaselineKey> baselineKeys = baselines.keySet().stream().sorted(BASELINE_ORDER).toList();
        List<AggregatedCohort> cohorts = new ArrayList<>();
        for (BaselineKey key : baselineKeys) {
            cohorts.add(cohort(key.championId(), key.role(), null, BuildScope.CHAMPION_ROLE,
                    baselines.get(key), window, ranker));
        }
        exacts.entrySet().stream().
                filter(entry -> rules.exactMatchupEligible(entry.getValue().size())).
                sorted(Map.Entry.comparingByKey(EXACT_ORDER)).
                map(entry -> cohort(entry.getKey().championId(), entry.getKey().role(),
                        entry.getKey().opponentChampionId(), BuildScope.EXACT_MATCHUP,
                        entry.getValue(), window, ranker)).
                forEach(cohorts::add);

        return new AggregationResult(cohorts, new LinkedHashSet<>(baselineKeys), eligible.size());
    }

    private AggregatedCohort cohort(
            int championId,
            BuildRole role,
            Integer opponentChampionId,
            BuildScope scope,
            List<BuildObservation> observations,
            PatchWindow window,
            BuildComponentRanker ranker
    ) {
        int games = observations.size();
        int wins = (int) observations.stream().filter(BuildObservation::win).count();
        int anchorGames = (int) observations.stream().
                filter(observation -> observation.patch().equals(window.anchorPatch())).
                count();

        List<BuildChoice> startingItems = ranker.rank(observations,
                observation -> choice(observation.items().startingItems()), 1);
        List<BuildChoice> boots = ranker.rank(observations,
                observation -> observation.items().boots() == null
                        ? List.of() : choice(List.of(observation.items().boots())), 1);
        List<BuildChoice> coreItems = ranker.rank(observations,
                observation -> choice(observation.items().coreItems()), 1);
        Set<Integer> topCoreIds = coreItems.isEmpty()
                ? Set.of() : new HashSet<>(coreItems.getFirst().ids());
        List<BuildChoice> situationalItems = ranker.rank(observations,
                observation -> observation.items().coreItems().stream().
                        filter(itemId -> !topCoreIds.contains(itemId)).
                        distinct().
                        map(List::of).
                        toList(), 10);
        List<BuildChoice> runePages = ranker.rank(observations,
                observation -> choice(runeIds(observation.runes())), 1);
        List<BuildChoice> spellPairs = ranker.rank(observations,
                observation -> choice(observation.spells().stream().sorted().toList()), 1);
        List<BuildChoice> skillOrders = ranker.rank(observations,
                observation -> validSkillOrder(observation.skills().order())
                        ? choice(observation.skills().order()) : List.of(), 1);
        List<BuildChoice> priorities = ranker.rank(observations,
                observation -> choice(skillMaxPriority(observation.skills().order())), 1);

        BuildSnapshotPayload payload = new BuildSnapshotPayload(
                startingItems, boots, coreItems, situationalItems, runePages,
                spellPairs, skillOrders,
                priorities.isEmpty() ? List.of() : priorities.getFirst().ids());
        return new AggregatedCohort(championId, role, opponentChampionId, scope,
                games, wins, anchorGames, games - anchorGames, rules.confidence(games), payload);
    }

    private static List<List<Integer>> choice(List<Integer> ids) {
        return ids.isEmpty() ? List.of() : List.of(ids);
    }

    private static List<Integer> runeIds(RunePage runes) {
        List<Integer> ids = new ArrayList<>();
        ids.add(runes.primaryStyleId());
        ids.addAll(runes.primarySelections());
        ids.add(runes.secondaryStyleId());
        ids.addAll(runes.secondarySelections());
        ids.addAll(runes.statShards());
        return ids;
    }

    private static boolean validSkillOrder(List<Integer> order) {
        return !order.isEmpty() && order.stream().allMatch(slot -> slot >= 1 && slot <= 4);
    }

    private static List<Integer> skillMaxPriority(List<Integer> order) {
        if (!validSkillOrder(order)) {
            return List.of();
        }
        int[] ranks = new int[4];
        List<Integer> priority = new ArrayList<>(3);
        for (int slot : order) {
            if (slot <= 3 && ++ranks[slot] == 5) {
                priority.add(slot);
            }
        }
        return priority.size() == 3 ? List.copyOf(priority) : List.of();
    }

    private record ExactKey(int championId, BuildRole role, int opponentChampionId) {
    }
}
