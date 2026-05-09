package org.main.dto.frontend;

public record PlayerSearchResultDto(
        String puuid,
        String gameName,
        String tagLine,
        Long matches
) {
}