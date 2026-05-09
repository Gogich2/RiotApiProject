package org.main.controller.frontend;

import org.main.dto.frontend.SearchResultDto;
import org.main.service.frontend.FrontendStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class FrontendSearchController {

    private final FrontendStatsService frontendStatsService;

    public FrontendSearchController(FrontendStatsService frontendStatsService) {
        this.frontendStatsService = frontendStatsService;
    }

    @GetMapping
    public SearchResultDto search(@RequestParam String query) {
        return frontendStatsService.search(query);
    }
}