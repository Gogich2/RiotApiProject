package org.main.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface RiotApiClient {

    /**
     * Modern identity (recommended): Riot ID = gameName#tagLine -> account info including puuid
     * Regional routing: EUROPE for EU accounts.
     */
    JsonNode getAccountByRiotIdEurope(String gameName, String tagLine);

    /**
     * Matchlist for a given puuid (regional routing).
     */
    List<String> getMatchIdsByPuuidEurope(String puuid, int start, int count);

    /**
     * Match details by matchId (regional routing).
     */
    JsonNode getMatchByIdEurope(String matchId);
}
