package org.main.service.frontend;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.main.client.RiotApiClient;
import org.main.dto.frontend.RiotIdResolveResponse;
import org.main.exception.NotFoundException;
import org.main.persistence.entity.PlayerEntity;
import org.main.persistence.repository.PlayerRepository;
import org.main.refresh.dto.PlayerRefreshStatusDto;
import org.main.refresh.entity.RefreshSource;
import org.main.refresh.service.PlayerRefreshCoordinator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiotIdResolveService {

    private final PlayerRepository playerRepository;

    private final RiotApiClient riotApiClient;

    private final PlayerRefreshCoordinator refreshCoordinator;

    private final Clock clock;

    public RiotIdResolveService(
            PlayerRepository playerRepository,
            RiotApiClient riotApiClient,
            PlayerRefreshCoordinator refreshCoordinator,
            Clock clock
    ) {
        this.playerRepository = playerRepository;
        this.riotApiClient = riotApiClient;
        this.refreshCoordinator = refreshCoordinator;
        this.clock = clock;
    }

    @Transactional
    public RiotIdResolveResponse resolve(String gameName, String tagLine) {
        return playerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase(gameName, tagLine).
                map(player -> response(player, null)).
                orElseGet(() -> resolveFromRiot(gameName, tagLine));
    }

    private RiotIdResolveResponse resolveFromRiot(String gameName, String tagLine) {
        JsonNode account = riotApiClient.getAccountByRiotIdEurope(gameName, tagLine);
        String puuid = text(account, "puuid");
        OffsetDateTime now = OffsetDateTime.now(clock);
        PlayerEntity player = new PlayerEntity();
        player.setPuuid(puuid);
        player.setGameName(account.path("gameName").asText(gameName));
        player.setTagLine(account.path("tagLine").asText(tagLine));
        player.setCreatedAt(now);
        player.setUpdatedAt(now);
        playerRepository.save(player);
        PlayerRefreshStatusDto refresh = refreshCoordinator.enqueue(puuid, RefreshSource.RESOLVE);
        return response(player, refresh);
    }

    private String text(JsonNode account, String field) {
        String value = account == null ? null : account.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw new NotFoundException("Riot account not found");
        }
        return value;
    }

    private RiotIdResolveResponse response(PlayerEntity player, PlayerRefreshStatusDto refresh) {
        return new RiotIdResolveResponse(
                player.getPuuid(),
                player.getGameName(),
                player.getTagLine(),
                refresh
        );
    }
}
