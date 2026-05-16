package org.main.service;

import java.util.List;
import org.main.persistence.entity.LeagueEntryEntity;

public interface RankEnrichmentService {

    List<LeagueEntryEntity> enrichRanksForPuuidEuw(String puuid);

    boolean hasRankData(String puuid);
}