package org.main.builds.source;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Set;
import org.main.builds.model.BuildQueue;

public record BuildSourceMatch(
        String matchId,
        String patch,
        BuildQueue queue,
        JsonNode timeline,
        List<Participant> participants
) {

    public BuildSourceMatch {
        participants = List.copyOf(participants);
    }

    public record Participant(
            int participantId,
            Integer teamId,
            Integer championId,
            String teamPosition,
            String individualPosition,
            Boolean win,
            Set<Integer> finalItemIds,
            JsonNode perks,
            List<Integer> spells,
            List<Integer> skills
    ) {

        public Participant {
            finalItemIds = Set.copyOf(finalItemIds);
            spells = List.copyOf(spells);
            skills = List.copyOf(skills);
        }
    }
}
