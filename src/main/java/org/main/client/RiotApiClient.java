package org.main.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.main.dto.CrawlResultDto;

import java.util.List;

public interface RiotApiClient {
    JsonNode getAccountByRiotIdEurope(String gameName, String tagLine);
    List<String> getMatchIdsByPuuidEurope(String puuid, int start, int count);
    JsonNode getMatchByIdEurope(String matchId);
}
