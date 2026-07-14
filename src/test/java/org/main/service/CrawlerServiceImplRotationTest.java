package org.main.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.main.client.RiotApiClient;
import org.main.persistence.entity.PlayerEntity;
import org.main.persistence.repository.MatchRepository;
import org.main.persistence.repository.PlayerRepository;
import org.springframework.transaction.support.TransactionTemplate;

class CrawlerServiceImplRotationTest {

    private RiotApiClient riotApiClient;

    private PlayerRepository playerRepository;

    private CrawlerServiceImpl service;

    @BeforeEach
    void setUp() {
        riotApiClient = mock(RiotApiClient.class);
        playerRepository = mock(PlayerRepository.class);
        service = new CrawlerServiceImpl(
                riotApiClient,
                mock(MatchRepository.class),
                playerRepository,
                mock(TimelineIngestService.class),
                mock(IngestLogService.class),
                mock(TransactionTemplate.class)
        );
    }

    @Test
    void returnsEmptyWhenNoPlayerIsStored() {
        when(playerRepository.findNextCrawlCandidate()).thenReturn(Optional.empty());

        assertThat(service.crawlNextPlayerEUW(20)).isEmpty();
    }

    @Test
    void updatesOnlyAttemptTimestampWhenCrawlSavesNothing() {
        PlayerEntity player = player("puuid-success");
        when(playerRepository.findNextCrawlCandidate()).thenReturn(Optional.of(player));
        when(riotApiClient.getMatchIdsByPuuidEurope("puuid-success", 0, 20)).
                thenReturn(List.of());
        OffsetDateTime before = OffsetDateTime.now();

        var result = service.crawlNextPlayerEUW(20);

        assertThat(result).isPresent();
        assertThat(result.get().savedNewMatches()).isZero();
        assertThat(player.getLastCrawlAttemptAt()).isNull();
        ArgumentCaptor<OffsetDateTime> attemptedAt = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(playerRepository).updateLastCrawlAttemptAt(eq("puuid-success"), attemptedAt.capture());
        assertThat(attemptedAt.getValue()).isAfterOrEqualTo(before);
        verify(playerRepository, never()).save(any(PlayerEntity.class));
    }

    @Test
    void updatesOnlyAttemptTimestampAndRethrowsWhenCrawlFails() {
        PlayerEntity player = player("puuid-failure");
        when(playerRepository.findNextCrawlCandidate()).thenReturn(Optional.of(player));
        when(riotApiClient.getMatchIdsByPuuidEurope("puuid-failure", 0, 20)).
                thenThrow(new IllegalStateException("Riot unavailable"));

        assertThatThrownBy(() -> service.crawlNextPlayerEUW(20)).
                isInstanceOf(IllegalStateException.class).
                hasMessage("Riot unavailable");
        assertThat(player.getLastCrawlAttemptAt()).isNull();
        verify(playerRepository).updateLastCrawlAttemptAt(
                eq("puuid-failure"),
                any(OffsetDateTime.class)
        );
        verify(playerRepository, never()).save(any(PlayerEntity.class));
    }

    private PlayerEntity player(String puuid) {
        PlayerEntity player = new PlayerEntity();
        player.setPuuid(puuid);
        player.setCreatedAt(OffsetDateTime.now().minusDays(1));
        player.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        return player;
    }

}
