package org.main.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;

@Component
public class RiotApiHttpClient implements RiotApiClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${riot.api.key}")
    private String apiKey;

    // EUW-only endpoints
    private static final String EUW_PLATFORM_HOST = "https://euw1.api.riotgames.com";
    private static final String EUROPE_REGIONAL_HOST = "https://europe.api.riotgames.com";

    @Override
    public JsonNode getSummonerByNameEUW(String summonerName) {
        String url = UriComponentsBuilder.fromHttpUrl(EUW_PLATFORM_HOST)
                .path("/lol/summoner/v4/summoners/by-name/{name}")
                .buildAndExpand(summonerName)
                .toUriString();

        return exchangeJson(url);
    }

    @Override
    public List<String> getMatchIdsByPuuidEurope(String puuid, int start, int count) {
        String url = UriComponentsBuilder.fromHttpUrl(EUROPE_REGIONAL_HOST)
                .path("/lol/match/v5/matches/by-puuid/{puuid}/ids")
                .queryParam("start", start)
                .queryParam("count", count)
                .buildAndExpand(puuid)
                .toUriString();

        ResponseEntity<String[]> resp = exchange(url, String[].class);
        String[] body = resp.getBody();
        return body == null ? List.of() : Arrays.asList(body);
    }

    @Override
    public JsonNode getMatchByIdEurope(String matchId) {
        String url = UriComponentsBuilder.fromHttpUrl(EUROPE_REGIONAL_HOST)
                .path("/lol/match/v5/matches/{matchId}")
                .buildAndExpand(matchId)
                .toUriString();

        return exchangeJson(url);
    }

    private JsonNode exchangeJson(String url) {
        ResponseEntity<JsonNode> resp = exchange(url, JsonNode.class);
        return resp.getBody();
    }

    private <T> ResponseEntity<T> exchange(String url, Class<T> clazz) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", apiKey);

        HttpEntity<Void> req = new HttpEntity<>(headers);

        return restTemplate.exchange(url, HttpMethod.GET, req, clazz);
    }
}
