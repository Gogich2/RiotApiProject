package org.main.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface RiotApiClient {
    JsonNode getSummonerByNameEUW(String summonerName);
    List<String> getMatchIdsByPuuidEurope(String puuid, int start, int count);
    JsonNode getMatchByIdEurope(String matchId);
}
