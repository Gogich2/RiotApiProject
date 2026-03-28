package org.main.client;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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

    // EU-only hosts
    private static final String EUW_PLATFORM_HOST = "https://euw1.api.riotgames.com";

    private static final String EUROPE_REGIONAL_HOST = "https://europe.api.riotgames.com";

    @PostConstruct
    void check() {
        System.out.println("RIOT_API_KEY length = " + (apiKey == null ? "null" : apiKey.length()));
        System.out.println("RIOT_API_KEY startsWith RGAPI- = " + (apiKey != null && apiKey.startsWith("RGAPI-")));
    }

    @Override
    public JsonNode getAccountByRiotIdEurope(String gameName, String tagLine) {
        String url = UriComponentsBuilder.fromHttpUrl(EUROPE_REGIONAL_HOST).
                path("/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}").
                buildAndExpand(gameName, tagLine).
                toUriString();

        return exchangeJson(url);
    }

    @Override
    public List<String> getMatchIdsByPuuidEurope(String puuid, int start, int count) {
        String url = UriComponentsBuilder.fromHttpUrl(EUROPE_REGIONAL_HOST).
                path("/lol/match/v5/matches/by-puuid/{puuid}/ids").
                queryParam("start", start).
                queryParam("count", count).
                buildAndExpand(puuid).
                toUriString();

        ResponseEntity<String[]> resp = exchange(url, String[].class);
        String[] body = resp.getBody();
        return body == null ? List.of() : Arrays.asList(body);
    }

    @Override
    public JsonNode getMatchByIdEurope(String matchId) {
        String url = UriComponentsBuilder.fromHttpUrl(EUROPE_REGIONAL_HOST).
                path("/lol/match/v5/matches/{matchId}").
                buildAndExpand(matchId).
                toUriString();

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
