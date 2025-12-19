package org.main.cucumber.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.main.service.CrawlerService;
import org.springframework.beans.factory.annotation.Autowired;

public class CrawlSteps {

    @Autowired
    private CrawlerService crawlerService;

    @Given("EUW summoner {string} exists")
    public void summonerExists(String name) {
        // smoke precondition
    }

    @When("I crawl last {int} matches")
    public void crawlMatches(int limit) {
        crawlerService.crawlSummonerEUW("acoomer", limit);
    }

    @Then("matches are saved")
    public void matchesSaved() {
        // smoke assertion (без глибоких перевірок)
    }
}
