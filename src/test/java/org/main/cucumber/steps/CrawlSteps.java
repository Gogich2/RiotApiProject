package org.main.cucumber.steps;

import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.main.client.RiotApiClient;
import org.main.persistence.repository.MatchRepository;
import org.main.service.CrawlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CrawlSteps {

    private static final String PUUID = "TEST_PUUID";

    private int limit;

    @Autowired
    private CrawlerService crawlerService;

    @Autowired
    private RiotApiClient riotApiClient;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    // ---------- GIVEN ----------

    @Given("EUW summoner {string} exists")
    public void summonerExists(String name) {
        doAnswer(invocation -> {
            invocation.getArgument(0, java.util.function.Consumer.class).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    // ---------- WHEN ----------

    @When("I crawl last {int} matches")
    public void crawlMatches(int limit) {
        this.limit = limit;

        List<String> ids = IntStream.range(0, limit).
                mapToObj(i -> "EUW1_" + i).
                toList();

        when(riotApiClient.getMatchIdsByPuuidEurope(eq(PUUID), anyInt(), anyInt())).
                thenReturn(ids).
                thenReturn(List.of());

        when(matchRepository.existsById(anyString())).thenReturn(false);
        when(riotApiClient.getMatchByIdEurope(anyString())).thenReturn(mock(JsonNode.class));

        crawlerService.crawlPuuidEUW(PUUID, limit);
    }

    @When("I crawl last {int} matches and no matches exist")
    public void crawlNoMatches(int limit) {
        this.limit = limit;

        when(riotApiClient.getMatchIdsByPuuidEurope(eq(PUUID), anyInt(), anyInt())).
                thenReturn(List.of());

        crawlerService.crawlPuuidEUW(PUUID, limit);
    }

    @When("I crawl last {int} matches but all already exist")
    public void crawlAllExisting(int limit) {
        this.limit = limit;

        List<String> ids = IntStream.range(0, limit).
                mapToObj(i -> "EUW1_" + i).
                toList();

        when(riotApiClient.getMatchIdsByPuuidEurope(eq(PUUID), anyInt(), anyInt())).
                thenReturn(ids).
                thenReturn(List.of());

        when(matchRepository.existsById(anyString())).thenReturn(true);

        crawlerService.crawlPuuidEUW(PUUID, limit);
    }

    // ---------- THEN ----------

    @Then("matches are saved")
    public void matchesSaved() {
        verify(matchRepository, times(limit)).save(any());
    }

    @Then("no matches are saved")
    public void noMatchesSaved() {
        verify(matchRepository, never()).save(any());
    }
}
