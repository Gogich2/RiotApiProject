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

    @PostMapping("/euw/summoner/{name}")
    public CrawlResultDto crawlSummoner(@PathVariable("name") String name,
                                        @RequestParam(value = "limit", required = false, defaultValue = "20") int limit) {
        return crawlerService.crawlSummonerEUW(name, limit);
    }
}
