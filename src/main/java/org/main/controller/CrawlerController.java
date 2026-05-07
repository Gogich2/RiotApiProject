package org.main.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.main.dto.CrawlResultDto;
import org.main.service.CrawlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контролер для запуску процесу збору та збереження даних з Riot API.
 */
@Tag(name = "Crawler API", description = "API для запуску збору та обробки даних з Riot API")
@RestController
@RequestMapping("/api/crawl")
public class CrawlerController {

    private static final Logger log = LoggerFactory.getLogger(CrawlerController.class);

    private final CrawlerService crawlerService;

    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @Operation(
            summary = "Запуск обробки матчів за PUUID",
            description = "Отримує матчі для вказаного PUUID у регіоні EUW, обробляє їх і зберігає"
    )
    @PostMapping("/euw/puuid/{puuid}")
    public CrawlResultDto crawlByPuuid(@PathVariable String puuid,
                                       @RequestParam(value = "limit", defaultValue = "20") int limit) {
        log.info("Received crawlByPuuid request: puuid='{}', limit={}", puuid, limit);
        CrawlResultDto result = crawlerService.crawlPuuidEUW(puuid, limit);
        log.info("crawlByPuuid completed: puuid='{}', requestedLimit={}, savedNewMatches={}",
                result.puuid(), result.requestedLimit(), result.savedNewMatches());
        return result;
    }

    @PostMapping("/euw/riotid/{gameName}/{tagLine}")
    public CrawlResultDto crawlByRiotId(@PathVariable String gameName,
                                        @PathVariable String tagLine,
                                        @RequestParam(value = "limit", defaultValue = "20") int limit) {
        log.info("Received crawlByRiotId request: gameName='{}', tagLine='{}', limit={}",
                gameName, tagLine, limit);
        CrawlResultDto result = crawlerService.crawlRiotIdEUW(gameName, tagLine, limit);
        log.info("crawlByRiotId completed: summoner='{}', puuid='{}', requestedLimit={}, savedNewMatches={}",
                result.summonerName(), result.puuid(), result.requestedLimit(), result.savedNewMatches());
        return result;
    }

    @PostMapping("/euw/latest-player")
    public CrawlResultDto crawlLatestPlayer(@RequestParam(value = "limit", defaultValue = "100") int limit) {
        log.info("Received crawlLatestPlayer request: limit={}", limit);

        CrawlResultDto result = crawlerService.crawlLatestPlayerEUW(limit);

        log.info(
                "crawlLatestPlayer completed: puuid='{}', requestedLimit={}, savedNewMatches={}",
                result.puuid(),
                result.requestedLimit(),
                result.savedNewMatches()
        );

        return result;
    }
}