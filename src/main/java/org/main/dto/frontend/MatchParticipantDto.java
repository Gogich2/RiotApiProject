package org.main.dto.frontend;

import java.util.List;

public record MatchParticipantDto(
        String matchId,
        Integer participantId,
        String puuid,
        String gameName,
        String tagLine,
        Integer championId,
        String championName,
        String championImageUrl,
        Integer teamId,
        Boolean win,
        Integer kills,
        Integer deaths,
        Integer assists,
        Double kda,
        Integer champLevel,
        Integer goldEarned,
        Integer totalDamageToChampions,
        Integer totalDamageTaken,
        Integer visionScore,
        Integer wardsPlaced,
        Integer wardsKilled,
        Integer totalMinionsKilled,
        Integer neutralMinionsKilled,
        Integer summoner1Id,
        Integer summoner2Id,
        List<PlayerMatchItemDto> finalItems,
        List<MatchParticipantRuneDto> runes,
        List<MatchParticipantSkillOrderDto> skillOrder,
        List<MatchParticipantItemEventDto> itemEvents
) {
}
