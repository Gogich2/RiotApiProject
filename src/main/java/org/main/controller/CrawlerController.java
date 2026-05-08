package org.main.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.main.dto.BalancedDatasetResultDto;
import org.main.dto.CrawlResultDto;
import org.main.exception.NotFoundException;
import org.main.persistence.entity.PlayerEntity;
import org.main.persistence.repository.PlayerRepository;
import org.main.service.BalancedDatasetCrawlerService;
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

    private final BalancedDatasetCrawlerService balancedDatasetCrawlerService;

    private final PlayerRepository playerRepository;

    public CrawlerController(CrawlerService crawlerService,
                             BalancedDatasetCrawlerService balancedDatasetCrawlerService,
                             PlayerRepository playerRepository) {
        this.crawlerService = crawlerService;
        this.balancedDatasetCrawlerService = balancedDatasetCrawlerService;
        this.playerRepository = playerRepository;
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

    @PostMapping("/euw/latest-player-balanced")
    public BalancedDatasetResultDto crawlBalancedFromLatestPlayer(
            @RequestParam(value = "targetPerBucket", defaultValue = "2000") int targetPerBucket,
            @RequestParam(value = "matchesPerPlayer", defaultValue = "100") int matchesPerPlayer,
            @RequestParam(value = "maxPlayersToVisit", defaultValue = "1000") int maxPlayersToVisit
    ) {
        PlayerEntity latestPlayer = playerRepository.findTopByOrderByUpdatedAtDesc().
                orElseThrow(() -> new NotFoundException("No players found in raw.players"));

        String puuid = latestPlayer.getPuuid();

        if (puuid == null || puuid.isBlank()) {
            throw new IllegalStateException("Latest player has empty PUUID");
        }

        log.info(
                "Received crawlBalancedFromLatestPlayer request: puuid='{}', targetPerBucket={}, "
                        + "matchesPerPlayer={}, maxPlayersToVisit={}",
                puuid,
                targetPerBucket,
                matchesPerPlayer,
                maxPlayersToVisit
        );

        BalancedDatasetResultDto result = balancedDatasetCrawlerService.collectBalancedDatasetEUW(
                List.of(puuid),
                targetPerBucket,
                matchesPerPlayer,
                maxPlayersToVisit
        );

        log.info(
                "crawlBalancedFromLatestPlayer completed: puuid='{}', visitedPlayers={}, "
                        + "scannedMatches={}, savedNewMatches={}, skippedMatches={}, balanced={}",
                puuid,
                result.visitedPlayers(),
                result.scannedMatches(),
                result.savedNewMatches(),
                result.skippedMatches(),
                result.balanced()
        );

        return result;
    }
}