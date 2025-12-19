package org.main.service;

import org.main.dto.CrawlResultDto;

public interface CrawlerService {
    CrawlResultDto crawlPuuidEUW(String puuid, int limitRaw);
}
