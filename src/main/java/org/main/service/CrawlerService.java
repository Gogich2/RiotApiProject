package org.main.service;

import org.main.dto.CrawlResultDto;

public interface CrawlerService {

    /**
     * Crawl starting from PUUID.
     */
    CrawlResultDto crawlPuuidEUW(String puuid, int limitRaw);

    /**
     * Crawl starting from RiotID (gameName + tagLine) -> puuid -> matches
     */
    CrawlResultDto crawlRiotIdEUW(String gameNameRaw, String tagLineRaw, int limitRaw);

    void crawlSummonerEUW(String acoomer, int limit);
}
