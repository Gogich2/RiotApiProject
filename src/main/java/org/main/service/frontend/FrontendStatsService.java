package org.main.service.frontend;

import org.main.dto.frontend.ChampionDetailsDto;
import org.main.dto.frontend.ChampionItemStatsDto;
import org.main.dto.frontend.ChampionSummaryDto;
import org.main.dto.frontend.OverviewStatsDto;
import org.main.dto.frontend.PlayerRecentMatchDto;
import org.main.dto.frontend.PlayerSummaryDto;
import org.main.dto.frontend.SearchResultDto;
import org.main.dto.frontend.PlayerInsightDto;
import org.main.dto.frontend.PlayerChampionStatsDto;

import java.util.List;

public interface FrontendStatsService {

    SearchResultDto search(String query);

    OverviewStatsDto getOverview();

    ChampionDetailsDto getChampionDetails(Integer championId);

    ChampionSummaryDto getChampionSummary(Integer championId);

    List<ChampionItemStatsDto> getChampionItems(Integer championId);

    PlayerSummaryDto getPlayerSummary(String puuid);

    List<PlayerRecentMatchDto> getPlayerRecentMatches(String puuid, int limit);

    List<PlayerInsightDto> getPlayerInsights(String puuid);

    List<PlayerChampionStatsDto> getPlayerChampions(String puuid);

}