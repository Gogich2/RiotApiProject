package org.main.refresh.web;

import org.main.account.dto.AuthErrorResponse;
import org.main.refresh.dto.PlayerRefreshStatusDto;
import org.main.refresh.entity.RefreshSource;
import org.main.refresh.service.PlayerRefreshCoordinator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players/{puuid}")
public class PlayerRefreshController {

    private final PlayerRefreshCoordinator coordinator;

    public PlayerRefreshController(PlayerRefreshCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @PostMapping("/refresh")
    public ResponseEntity<PlayerRefreshStatusDto> refresh(@PathVariable String puuid) {
        return ResponseEntity.accepted().body(coordinator.enqueue(puuid, RefreshSource.MANUAL));
    }

    @GetMapping("/refresh-status")
    public PlayerRefreshStatusDto status(@PathVariable String puuid) {
        return coordinator.latest(puuid);
    }

    @ExceptionHandler(PlayerRefreshCoordinator.RefreshCooldownException.class)
    public ResponseEntity<AuthErrorResponse> cooldown(
            PlayerRefreshCoordinator.RefreshCooldownException exception
    ) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).
                header(HttpHeaders.RETRY_AFTER, Long.toString(exception.getRetryAfter().toSeconds())).
                body(new AuthErrorResponse(exception.getMessage()));
    }
}
