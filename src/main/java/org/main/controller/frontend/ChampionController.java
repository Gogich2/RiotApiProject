package org.main.controller.frontend;

import org.main.dto.frontend.ChampionDetailsDto;
import org.main.dto.frontend.ChampionItemStatsDto;
import org.main.dto.frontend.ChampionStatDto;
import org.main.dto.frontend.ChampionSummaryDto;
import org.main.service.frontend.FrontendStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/champions")
public class ChampionController {

    private final FrontendStatsService frontendStatsService;

    public ChampionController(FrontendStatsService frontendStatsService) {
        this.frontendStatsService = frontendStatsService;
    }

    @GetMapping
    public List<ChampionStatDto> champions() {
        return frontendStatsService.getChampions();
    }

    @GetMapping("/{championId}")
    public ChampionDetailsDto details(@PathVariable Integer championId) {
        return frontendStatsService.getChampionDetails(championId);
    }

    @GetMapping("/{championId}/summary")
    public ChampionSummaryDto summary(@PathVariable Integer championId) {
        return frontendStatsService.getChampionSummary(championId);
    }

    @GetMapping("/{championId}/items")
    public List<ChampionItemStatsDto> items(@PathVariable Integer championId) {
        return frontendStatsService.getChampionItems(championId);
    }
}
