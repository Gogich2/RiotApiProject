package org.main.dto;

import java.util.List;

public record CrawlResultDto(
        String platform,
        String summonerName,
        String puuid,
        int requestedLimit,
        int savedNewMatches,
        List<String> savedMatchIds
) {}
