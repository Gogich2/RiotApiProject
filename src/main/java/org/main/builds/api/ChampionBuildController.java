package org.main.builds.api;

import org.main.builds.model.BuildRole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/champions/{championId}/builds")
public final class ChampionBuildController {

    private final ChampionBuildService service;

    public ChampionBuildController(ChampionBuildService service) {
        this.service = service;
    }

    @GetMapping("/options")
    public ChampionBuildOptionsResponse options(
            @PathVariable int championId,
            @RequestParam(required = false) Integer queueId,
            @RequestParam(required = false) String patch,
            @RequestParam(required = false) BuildRole role
    ) {
        return service.options(championId, queueId, patch, role);
    }

    @GetMapping
    public ChampionBuildResponse builds(
            @PathVariable int championId,
            @RequestParam int queueId,
            @RequestParam String patch,
            @RequestParam BuildRole role,
            @RequestParam(required = false) Integer opponentId
    ) {
        return service.builds(championId, queueId, patch, role, opponentId);
    }
}
