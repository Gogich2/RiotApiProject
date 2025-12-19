package org.main.service;

import org.main.dto.CrawlResultDto;

public interface CrawlerService {
    CrawlResultDto crawlSummonerEUW(String summonerName, int limit);
}
