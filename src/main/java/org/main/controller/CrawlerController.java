package org.main.controller;

import org.main.dto.CrawlResultDto;
import org.main.service.CrawlerService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crawl")
public class CrawlerController {

    private final CrawlerService crawlerService;

    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

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
