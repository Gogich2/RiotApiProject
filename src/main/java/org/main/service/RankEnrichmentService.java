package org.main.service;

public interface RankEnrichmentService {

    RankEnrichmentResult enrichRanksForPuuidEuw(String puuid);

    boolean hasRankData(String puuid);
}