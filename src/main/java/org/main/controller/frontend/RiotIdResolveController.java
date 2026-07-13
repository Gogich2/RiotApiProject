package org.main.controller.frontend;

import jakarta.validation.Valid;
import org.main.dto.frontend.RiotIdResolveRequest;
import org.main.dto.frontend.RiotIdResolveResponse;
import org.main.service.frontend.RiotIdResolveService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players")
public class RiotIdResolveController {

    private final RiotIdResolveService resolveService;

    public RiotIdResolveController(RiotIdResolveService resolveService) {
        this.resolveService = resolveService;
    }

    @PostMapping("/resolve")
    public RiotIdResolveResponse resolve(@Valid @RequestBody RiotIdResolveRequest request) {
        return resolveService.resolve(request.gameName(), request.tagLine());
    }
}
