package org.main.service;

import org.main.dto.CrawlResultDto;

public interface CrawlerService {

    /**
     * Crawl starting from PUUID (works even if you don't know RiotID).
     */
    CrawlResultDto crawlPuuidEUW(String puuid, int limitRaw);

    /**
     * Crawl starting from RiotID (gameName + tagLine) -> puuid -> matches
     * Recommended modern entrypoint.
     */
    CrawlResultDto crawlRiotIdEUW(String gameNameRaw, String tagLineRaw, int limitRaw);

    void crawlSummonerEUW(String acoomer, int limit);
}
