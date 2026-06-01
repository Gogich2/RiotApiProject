package org.main.dto.frontend;

import java.util.List;

public record MatchDetailsDto(
        MatchSummaryDto match,
        MatchParticipantDto selectedParticipant,
        List<MatchParticipantDto> participants,
        List<MatchTeamDto> teams,
        List<MatchTimelineEventDto> timelineEvents,
        MatchMetricsDto metrics
) {
}
