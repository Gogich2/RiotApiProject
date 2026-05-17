package org.main.dto;

public record RankRepairResultDto(
        int checkedPlayers,
        int enrichedPlayers,
        int changedPlayers
) {
}