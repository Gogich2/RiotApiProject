package org.main.client;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import org.main.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class RiotApiHttpClient implements RiotApiClient {

    private static final Logger log = LoggerFactory.getLogger(RiotApiHttpClient.class);

    private static final String EUROPE_REGIONAL_HOST = "https://europe.api.riotgames.com";

    private static final String EUW_PLATFORM_HOST = "https://euw1.api.riotgames.com";

    private final RestTemplate restTemplate = new RestTemplate();

    private final RiotRateLimiter riotRateLimiter;

    @Value("${riot.api.key}")
    private String apiKey;

    @Value("${app.logging.mask-riot-api-key:true}")
    private boolean maskApiKey;

    @Value("${riot.rate-limit.max-retries:5}")
    private int maxRetries;

    public RiotApiHttpClient(RiotRateLimiter riotRateLimiter) {
        this.riotRateLimiter = riotRateLimiter;
    }

    @PostConstruct
    void check() {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("Riot API key is missing");
            return;
        }

        String visiblePart = apiKey.length() >= 8 ? apiKey.substring(0, 8) : apiKey;
        String displayed = maskApiKey ? visiblePart + "***" : apiKey;

        log.info("Riot API client initialized. Key prefix='{}', startsWithRGAPI={}",
                displayed, apiKey.startsWith("RGAPI-"));
    }

    @Override
    public JsonNode getAccountByRiotIdEurope(String gameName, String tagLine) {
        String url = UriComponentsBuilder.fromHttpUrl(EUROPE_REGIONAL_HOST).
                path("/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}").
                buildAndExpand(gameName, tagLine).
                toUriString();

        log.debug("Calling Riot account endpoint: gameName='{}', tagLine='{}'", gameName, tagLine);
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

        log.debug("Calling Riot match ids endpoint: puuid='{}', start={}, count={}", puuid, start, count);

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

        log.debug("Calling Riot match details endpoint: matchId='{}'", matchId);
        return exchangeJson(url);
    }

    @Override
    public JsonNode getMatchTimelineByIdEurope(String matchId) {
        String url = UriComponentsBuilder.fromHttpUrl(EUROPE_REGIONAL_HOST).
                path("/lol/match/v5/matches/{matchId}/timeline").
                buildAndExpand(matchId).
                toUriString();

        log.debug("Calling Riot match timeline endpoint: matchId='{}'", matchId);
        return exchangeJson(url);
    }

    @Override
    public JsonNode getSummonerByPuuidEuw(String puuid) {
        String url = UriComponentsBuilder.fromHttpUrl(EUW_PLATFORM_HOST).
                path("/lol/summoner/v4/summoners/by-puuid/{puuid}").
                buildAndExpand(puuid).
                toUriString();

        log.debug("Calling Riot summoner endpoint: puuid='{}'", puuid);
        return exchangeJson(url);
    }

    @Override
    public JsonNode getLeagueEntriesByPuuidEuw(String puuid) {
        String url = UriComponentsBuilder.fromHttpUrl(EUW_PLATFORM_HOST).
                path("/lol/league/v4/entries/by-puuid/{puuid}").
                buildAndExpand(puuid).
                toUriString();

        log.debug("Calling Riot league entries endpoint: puuid='{}'", puuid);
        return exchangeJson(url);
    }

    private JsonNode exchangeJson(String url) {
        ResponseEntity<JsonNode> resp = exchange(url, JsonNode.class);
        return resp.getBody();
    }

    private <T> ResponseEntity<T> exchange(String url, Class<T> clazz) {
        int attempts = Math.max(maxRetries, 1);

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                riotRateLimiter.acquire();

                HttpHeaders headers = new HttpHeaders();
                headers.set("X-Riot-Token", apiKey);

                HttpEntity<Void> req = new HttpEntity<>(headers);
                ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, req, clazz);

                log.debug("External HTTP call succeeded: url='{}', status={}", url, response.getStatusCode());
                return response;
            } catch (HttpStatusCodeException ex) {
                int statusCode = ex.getStatusCode().value();

                if (statusCode == 429) {
                    String retryAfter = ex.getResponseHeaders() == null
                            ? null
                            : ex.getResponseHeaders().getFirst("Retry-After");

                    log.warn("Riot API rate limit reached: attempt={}/{}, retryAfter='{}', url='{}'",
                            attempt, attempts, retryAfter, url);

                    riotRateLimiter.pauseAfterRetryAfterHeader(retryAfter);

                    if (attempt < attempts) {
                        continue;
                    }
                }

                log.error("External HTTP call failed: url='{}', status={}, responseBody='{}'",
                        url, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);

                throw new ExternalServiceException("Riot API returned HTTP error: " + ex.getStatusCode(), ex);
            } catch (ResourceAccessException ex) {
                log.error("External HTTP call failed due to connectivity issue: url='{}'", url, ex);
                throw new ExternalServiceException("Unable to reach Riot API", ex);
            } catch (Exception ex) {
                log.error("Unexpected error during Riot API call: url='{}'", url, ex);
                throw new ExternalServiceException("Unexpected error during Riot API call", ex);
            }
        }

        throw new ExternalServiceException("Riot API request failed after retries");
    }
}