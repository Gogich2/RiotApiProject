package org.main.service.frontend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.main.dto.frontend.PlayerDashboardDto;
import org.main.dto.frontend.PlayerRecentMatchDto;
import org.main.dto.frontend.PlayerSummaryDto;
import org.main.persistence.entity.LeagueEntryEntity;
import org.main.persistence.entity.PlayerEntity;
import org.main.persistence.repository.LeagueEntryRepository;
import org.main.persistence.repository.PlayerRepository;
import org.main.refresh.entity.PlayerRefreshJobEntity;
import org.main.refresh.entity.RefreshSource;
import org.main.refresh.entity.RefreshState;
import org.main.refresh.repository.PlayerRefreshJobRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerDashboardServiceTest {

    private static final Instant INSTANT = Instant.parse("2026-07-13T06:00:00Z");

    @Mock
    private FrontendStatsService frontendStatsService;

    @Mock
    private LeagueEntryRepository leagueEntryRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerRefreshJobRepository refreshJobRepository;

    private PlayerDashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new PlayerDashboardServiceImpl(
                frontendStatsService,
                leagueEntryRepository,
                playerRepository,
                refreshJobRepository,
                Clock.fixed(INSTANT, ZoneOffset.UTC),
                Duration.ofHours(6)
        );
    }

    @Test
    void calculatesRecentWindowsChampionHealthPrioritiesAndFreshness() {
        when(frontendStatsService.getPlayerSummary("puuid")).thenReturn(summary());
        when(frontendStatsService.getPlayerRecentMatches("puuid", 20, 420)).thenReturn(matches());
        when(frontendStatsService.getPlayerChampions("puuid", 420)).thenReturn(List.of());
        LeagueEntryEntity rank = new LeagueEntryEntity();
        rank.setQueueType("RANKED_SOLO_5x5");
        rank.setTier("GOLD");
        rank.setRankValue("II");
        rank.setLeaguePoints(44);
        rank.setWins(20);
        rank.setLosses(15);
        rank.setLastSyncedAt(OffsetDateTime.parse("2026-07-13T05:50:00Z"));
        when(leagueEntryRepository.findByPuuidOrderByQueueTypeAsc("puuid")).thenReturn(List.of(rank));
        PlayerEntity player = new PlayerEntity();
        player.setUpdatedAt(OffsetDateTime.parse("2026-07-13T05:40:00Z"));
        when(playerRepository.findById("puuid")).thenReturn(Optional.of(player));
        when(refreshJobRepository.findFirstByPuuidOrderByRequestedAtDesc("puuid")).
                thenReturn(Optional.of(refreshJob()));

        PlayerDashboardDto dashboard = dashboardService.getDashboard("puuid");

        assertThat(dashboard.analysisQueue()).isEqualTo("Solo/Duo");
        assertThat(dashboard.analysisQueueId()).isEqualTo(420);
        assertThat(dashboard.recentForm()).extracting(form -> form.window()).containsExactly(5, 10, 20);
        assertThat(dashboard.recentForm().get(0).wins()).isEqualTo(3);
        assertThat(dashboard.recentForm().get(0).losses()).isEqualTo(2);
        assertThat(dashboard.recentForm().get(0).winRate()).isEqualTo(60.0);
        assertThat(dashboard.recentForm().get(0).averageKda()).isEqualTo(5.0);
        assertThat(dashboard.championPoolHealth().status()).isEqualTo("OVEREXTENDED");
        assertThat(dashboard.championPoolHealth().uniqueChampions()).isEqualTo(6);
        assertThat(dashboard.priorities()).hasSize(3).
                allSatisfy(priority -> assertThat(priority.description()).contains("Solo/Duo"));
        assertThat(dashboard.freshness().lastUpdatedAt()).isEqualTo(
                OffsetDateTime.parse("2026-07-13T05:55:00Z")
        );
        assertThat(dashboard.freshness().sampleSize()).isEqualTo(20);
        assertThat(dashboard.refresh().state()).isEqualTo(RefreshState.QUEUED);
    }

    @Test
    void usesFlexDataWithoutMixingItWithSoloDuo() {
        when(frontendStatsService.getPlayerSummary("puuid")).thenReturn(summary());
        when(frontendStatsService.getPlayerRecentMatches("puuid", 20, 440)).thenReturn(List.of());
        when(frontendStatsService.getPlayerChampions("puuid", 440)).thenReturn(List.of());
        LeagueEntryEntity flex = new LeagueEntryEntity();
        flex.setQueueType("RANKED_FLEX_SR");
        when(leagueEntryRepository.findByPuuidOrderByQueueTypeAsc("puuid")).thenReturn(List.of(flex));
        when(playerRepository.findById("puuid")).thenReturn(Optional.empty());
        when(refreshJobRepository.findFirstByPuuidOrderByRequestedAtDesc("puuid")).
                thenReturn(Optional.empty());

        PlayerDashboardDto dashboard = dashboardService.getDashboard("puuid", 440);

        assertThat(dashboard.analysisQueue()).isEqualTo("Flex");
        assertThat(dashboard.analysisQueueId()).isEqualTo(440);
    }

    private List<PlayerRecentMatchDto> matches() {
        List<PlayerRecentMatchDto> matches = new ArrayList<>();
        long newest = Instant.parse("2026-07-13T05:55:00Z").toEpochMilli();
        for (int index = 0; index < 20; index++) {
            matches.add(new PlayerRecentMatchDto(
                    "match-" + index,
                    index % 6,
                    "Champion " + index % 6,
                    null,
                    index % 2 == 0,
                    5,
                    2,
                    5,
                    420,
                    "16.13",
                    newest - index * 60_000L,
                    1_800_000L,
                    List.of()
            ));
        }
        return matches;
    }

    private PlayerSummaryDto summary() {
        return new PlayerSummaryDto(
                "puuid",
                "Player",
                "EUW",
                123,
                20L,
                10L,
                50.0,
                5.0,
                2.0,
                5.0,
                10_000.0,
                20_000.0,
                20.0
        );
    }

    private PlayerRefreshJobEntity refreshJob() {
        PlayerRefreshJobEntity job = new PlayerRefreshJobEntity();
        job.setId(UUID.fromString("f85fc970-d1fa-48fd-9995-af203109f350"));
        job.setPuuid("puuid");
        job.setSource(RefreshSource.MANUAL);
        job.setState(RefreshState.QUEUED);
        job.setRequestedAt(OffsetDateTime.parse("2026-07-13T05:59:00Z"));
        return job;
    }
}
