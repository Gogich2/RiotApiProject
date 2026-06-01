package org.main.dto.frontend;

import java.util.List;

public record MatchTeamDto(
        Integer teamId,
        String teamName,
        Boolean win,
        List<MatchParticipantDto> participants
) {
}
