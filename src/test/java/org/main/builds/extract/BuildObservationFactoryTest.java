package org.main.builds.extract;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.main.builds.model.BuildObservation;
import org.main.builds.model.BuildQueue;
import org.main.builds.model.BuildRole;
import org.main.builds.source.BuildSourceMatch;
import org.main.builds.source.ItemCatalog;

class BuildObservationFactoryTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private final JsonNode perks;

    BuildObservationFactoryTest() throws IOException {
        perks = mapper.readTree(getClass().getResourceAsStream("/builds/participant-perks.json"));
    }

    @Test
    void normalizesRolesPairsExactlyOneOpponentAndKeepsCompleteInputs() {
        BuildSourceMatch match = match(List.of(
                participant(1, 100, 11, "", "MIDDLE", true),
                participant(2, 200, 22, "MIDDLE", null, false)
        ));

        List<BuildObservation> observations = factory().from(match);

        assertThat(observations).hasSize(2);
        BuildObservation first = observations.getFirst();
        assertThat(first.role()).isEqualTo(BuildRole.MIDDLE);
        assertThat(first.opponentChampionId()).isEqualTo(22);
        assertThat(first.runes().statShards()).containsExactly(5005, 5008, 5002);
        assertThat(first.spells()).containsExactly(4, 14);
        assertThat(first.skills().order()).containsExactly(1, 2, 3, 1, 4, 1);
    }

    @Test
    void retainsBaselineWhenSameRoleOpponentIsMissingOrAmbiguous() {
        BuildSourceMatch noOpponent = match(List.of(
                participant(1, 100, 11, "TOP", null, true),
                participant(2, 200, 22, "JUNGLE", null, false)
        ));
        BuildSourceMatch twoOpponents = match(List.of(
                participant(1, 100, 11, "TOP", null, true),
                participant(2, 200, 22, "TOP", null, false),
                participant(3, 200, 33, "TOP", null, false)
        ));

        assertThat(factory().from(noOpponent).getFirst().opponentChampionId()).isNull();
        assertThat(factory().from(twoOpponents).getFirst().opponentChampionId()).isNull();
    }

    @Test
    void excludesMissingStateInvalidRoleAndInvalidRequiredComponents() {
        BuildSourceMatch match = match(List.of(
                participant(1, 100, null, "TOP", null, true),
                participant(2, 100, 12, "NONE", null, true),
                participant(3, 100, 13, "TOP", null, null),
                withSkills(participant(4, 100, 14, "TOP", null, true), List.of(1, 9)),
                withFinalItems(participant(5, 100, 15, "TOP", null, true), Set.of(1038)),
                withSpells(participant(6, 100, 16, "TOP", null, true), List.of(4)),
                withPerks(participant(7, 100, 17, "TOP", null, true), mapper.nullNode())
        ));

        assertThat(factory().from(match)).isEmpty();
    }

    private BuildObservationFactory factory() {
        ItemCatalog catalog = new ItemCatalog() {
            @Override
            public boolean isStartingItem(int id) {
                return id == 1055;
            }

            @Override
            public boolean isCompletedBoot(int id) {
                return id == 3006;
            }

            @Override
            public boolean isCompletedCoreItem(int id) {
                return id == 6672;
            }
        };
        return new BuildObservationFactory(
                new ItemSequenceExtractor(Duration.ofMinutes(2)),
                new RunePageExtractor(), new SkillPathExtractor(), catalog);
    }

    private BuildSourceMatch match(List<BuildSourceMatch.Participant> participants) {
        return new BuildSourceMatch("EUW1_test", "16.13", BuildQueue.SOLO_DUO,
                timelineFor(participants), participants);
    }

    private BuildSourceMatch.Participant participant(int id, int team, Integer champion,
                                                      String teamPosition, String individualPosition,
                                                      Boolean win) {
        return new BuildSourceMatch.Participant(id, team, champion, teamPosition, individualPosition,
                win, Set.of(3006, 6672), perks, List.of(4, 14), List.of(1, 2, 3, 1, 4, 1));
    }

    private BuildSourceMatch.Participant withSkills(BuildSourceMatch.Participant participant,
                                                     List<Integer> skills) {
        return new BuildSourceMatch.Participant(participant.participantId(), participant.teamId(),
                participant.championId(), participant.teamPosition(), participant.individualPosition(),
                participant.win(), participant.finalItemIds(), participant.perks(), participant.spells(), skills);
    }

    private BuildSourceMatch.Participant withFinalItems(BuildSourceMatch.Participant participant,
                                                         Set<Integer> items) {
        return new BuildSourceMatch.Participant(participant.participantId(), participant.teamId(),
                participant.championId(), participant.teamPosition(), participant.individualPosition(),
                participant.win(), items, participant.perks(), participant.spells(), participant.skills());
    }

    private BuildSourceMatch.Participant withSpells(BuildSourceMatch.Participant participant,
                                                     List<Integer> spells) {
        return new BuildSourceMatch.Participant(participant.participantId(), participant.teamId(),
                participant.championId(), participant.teamPosition(), participant.individualPosition(),
                participant.win(), participant.finalItemIds(), participant.perks(), spells, participant.skills());
    }

    private BuildSourceMatch.Participant withPerks(BuildSourceMatch.Participant participant,
                                                    JsonNode participantPerks) {
        return new BuildSourceMatch.Participant(participant.participantId(), participant.teamId(),
                participant.championId(), participant.teamPosition(), participant.individualPosition(),
                participant.win(), participant.finalItemIds(), participantPerks,
                participant.spells(), participant.skills());
    }

    private JsonNode timelineFor(List<BuildSourceMatch.Participant> participants) {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode events = root.putObject("info").putArray("frames").addObject().putArray("events");
        for (BuildSourceMatch.Participant participant : participants) {
            events.addObject().put("type", "ITEM_PURCHASED").
                    put("participantId", participant.participantId()).put("itemId", 1055).put("timestamp", 1000);
            events.addObject().put("type", "ITEM_PURCHASED").
                    put("participantId", participant.participantId()).put("itemId", 3006).put("timestamp", 130000);
            events.addObject().put("type", "ITEM_PURCHASED").
                    put("participantId", participant.participantId()).put("itemId", 6672).put("timestamp", 200000);
        }
        return root;
    }
}
