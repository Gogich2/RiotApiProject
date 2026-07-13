package org.main.builds.extract;

import java.util.ArrayList;
import java.util.List;
import org.main.builds.model.BuildObservation;
import org.main.builds.model.BuildRole;
import org.main.builds.model.ItemPath;
import org.main.builds.model.RunePage;
import org.main.builds.model.SkillPath;
import org.main.builds.source.BuildSourceMatch;
import org.main.builds.source.ItemCatalog;

public final class BuildObservationFactory {

    private final ItemSequenceExtractor itemExtractor;

    private final RunePageExtractor runeExtractor;

    private final SkillPathExtractor skillExtractor;

    private final ItemCatalog itemCatalog;

    public BuildObservationFactory(ItemSequenceExtractor itemExtractor,
                                   RunePageExtractor runeExtractor,
                                   SkillPathExtractor skillExtractor,
                                   ItemCatalog itemCatalog) {
        this.itemExtractor = itemExtractor;
        this.runeExtractor = runeExtractor;
        this.skillExtractor = skillExtractor;
        this.itemCatalog = itemCatalog;
    }

    public List<BuildObservation> from(BuildSourceMatch match) {
        List<ValidParticipant> valid = new ArrayList<>();
        for (BuildSourceMatch.Participant participant : match.participants()) {
            try {
                valid.add(validate(match, participant));
            } catch (IllegalArgumentException exception) {
                // Invalid source participants do not poison other observations from the match.
            }
        }

        List<BuildObservation> observations = new ArrayList<>();
        for (ValidParticipant participant : valid) {
            List<ValidParticipant> opponents = valid.stream().
                    filter(candidate -> !candidate.teamId().equals(participant.teamId())).
                    filter(candidate -> candidate.role() == participant.role()).
                    toList();
            Integer opponentChampionId = opponents.size() == 1
                    ? opponents.getFirst().championId() : null;
            observations.add(new BuildObservation(match.matchId(), match.patch(), match.queue(),
                    participant.championId(), participant.role(), opponentChampionId,
                    participant.win(), participant.items(), participant.runes(),
                    participant.spells(), participant.skills()));
        }
        return List.copyOf(observations);
    }

    private ValidParticipant validate(BuildSourceMatch match,
                                      BuildSourceMatch.Participant participant) {
        if (participant.teamId() == null || participant.championId() == null || participant.win() == null) {
            throw new IllegalArgumentException("Participant state is incomplete");
        }
        BuildRole role = BuildRole.fromParticipant(
                participant.teamPosition(), participant.individualPosition());
        if (participant.spells().size() != 2
                || participant.spells().stream().anyMatch(spell -> spell == null || spell <= 0)) {
            throw new IllegalArgumentException("Summoner spells are invalid");
        }
        ItemPath items = itemExtractor.extract(match.timeline(), participant.participantId(),
                participant.finalItemIds(), itemCatalog);
        if (items.startingItems().isEmpty()
                || items.boots() == null && items.coreItems().isEmpty()) {
            throw new IllegalArgumentException("Item path is incomplete");
        }
        RunePage runes = runeExtractor.extract(participant.perks());
        SkillPath skills = skillExtractor.extract(participant.skills());
        return new ValidParticipant(participant.teamId(), participant.championId(), role,
                participant.win(), items, runes, participant.spells(), skills);
    }

    private record ValidParticipant(
            Integer teamId,
            int championId,
            BuildRole role,
            boolean win,
            ItemPath items,
            RunePage runes,
            List<Integer> spells,
            SkillPath skills
    ) {
    }
}
