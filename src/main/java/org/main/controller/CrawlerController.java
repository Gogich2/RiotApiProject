package org.main.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.main.dto.CrawlResultDto;
import org.main.service.CrawlerService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контролер для запуску процесу збору та збереження даних з Riot API.
 * <p>
 * Надає HTTP-ендпоінти для ініціації обробки матчів за PUUID.
 */

@Tag(name = "Crawler API", description = "API для запуску збору та обробки даних з Riot API")
@RestController
@RequestMapping("/api/crawl")
public class CrawlerController {

    private final CrawlerService crawlerService;

    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @Operation(
            summary = "Запуск обробки матчів за PUUID",
            description = "Отримує матчі для вказаного PUUID у регіоні EUW, обробляє їх і зберігає "
    )
    @PostMapping("/euw/puuid/{puuid}")
    public CrawlResultDto crawlByPuuid(@PathVariable String puuid,
                                       @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return crawlerService.crawlPuuidEUW(puuid, limit);
    }

    @PostMapping("/euw/riotid/{gameName}/{tagLine}")
    public CrawlResultDto crawlByRiotId(@PathVariable String gameName,
                                        @PathVariable String tagLine,
                                        @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return crawlerService.crawlRiotIdEUW(gameName, tagLine, limit);
    }
}
