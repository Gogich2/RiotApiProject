package org.main.service.frontend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.main.client.RiotApiClient;
import org.main.dto.frontend.RiotIdResolveResponse;
import org.main.persistence.entity.PlayerEntity;
import org.main.persistence.repository.PlayerRepository;
import org.main.refresh.dto.PlayerRefreshStatusDto;
import org.main.refresh.entity.RefreshSource;
import org.main.refresh.entity.RefreshState;
import org.main.refresh.service.PlayerRefreshCoordinator;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RiotIdResolveServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private RiotApiClient riotApiClient;

    @Mock
    private PlayerRefreshCoordinator refreshCoordinator;

    private RiotIdResolveService resolveService;

    @BeforeEach
    void setUp() {
        resolveService = new RiotIdResolveService(
                playerRepository,
                riotApiClient,
                refreshCoordinator,
                Clock.fixed(Instant.parse("2026-07-13T06:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void returnsStoredPlayerWithoutCallingRiot() {
        PlayerEntity player = player("stored-puuid", "Player", "EUW");
        when(playerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase("Player", "EUW")).
                thenReturn(Optional.of(player));

        RiotIdResolveResponse response = resolveService.resolve("Player", "EUW");

        assertThat(response.puuid()).isEqualTo("stored-puuid");
        verify(riotApiClient, never()).getAccountByRiotIdEurope(any(), any());
    }

    @Test
    void persistsUnknownRiotIdAndQueuesIngestionWithoutWaiting() throws Exception {
        when(playerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase("Player", "EUW")).
                thenReturn(Optional.empty());
        when(riotApiClient.getAccountByRiotIdEurope("Player", "EUW")).thenReturn(
                new ObjectMapper().readTree("""
                        {"puuid":"new-puuid","gameName":"Canonical","tagLine":"EUW"}
                        """)
        );
        when(playerRepository.save(any(PlayerEntity.class))).
                thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshCoordinator.enqueue("new-puuid", RefreshSource.RESOLVE)).thenReturn(refreshStatus());

        RiotIdResolveResponse response = resolveService.resolve("Player", "EUW");

        assertThat(response.puuid()).isEqualTo("new-puuid");
        assertThat(response.gameName()).isEqualTo("Canonical");
        assertThat(response.refresh().state()).isEqualTo(RefreshState.QUEUED);
        verify(playerRepository).save(any(PlayerEntity.class));
    }

    private PlayerEntity player(String puuid, String gameName, String tagLine) {
        PlayerEntity player = new PlayerEntity();
        player.setPuuid(puuid);
        player.setGameName(gameName);
        player.setTagLine(tagLine);
        player.setCreatedAt(OffsetDateTime.parse("2026-07-13T05:00:00Z"));
        player.setUpdatedAt(OffsetDateTime.parse("2026-07-13T05:00:00Z"));
        return player;
    }

    private PlayerRefreshStatusDto refreshStatus() {
        return new PlayerRefreshStatusDto(
                UUID.fromString("f85fc970-d1fa-48fd-9995-af203109f350"),
                "new-puuid",
                RefreshSource.RESOLVE,
                RefreshState.QUEUED,
                OffsetDateTime.parse("2026-07-13T06:00:00Z"),
                null,
                null,
                null,
                null
        );
    }
}
