package org.main.service.frontend;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.main.dto.frontend.ChampionPoolHealthDto;
import org.main.dto.frontend.PlayerChampionStatsDto;
import org.main.dto.frontend.PlayerDashboardDto;
import org.main.dto.frontend.PlayerFreshnessDto;
import org.main.dto.frontend.PlayerInsightDto;
import org.main.dto.frontend.PlayerRankSummaryDto;
import org.main.dto.frontend.PlayerRecentMatchDto;
import org.main.dto.frontend.RecentFormDto;
import org.main.persistence.entity.LeagueEntryEntity;
import org.main.persistence.repository.LeagueEntryRepository;
import org.main.persistence.repository.PlayerRepository;
import org.main.refresh.dto.PlayerRefreshStatusDto;
import org.main.refresh.repository.PlayerRefreshJobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PlayerDashboardServiceImpl implements PlayerDashboardService {

    private static final List<Integer> FORM_WINDOWS = List.of(5, 10, 20);

    private final FrontendStatsService frontendStatsService;

    private final LeagueEntryRepository leagueEntryRepository;

    private final PlayerRepository playerRepository;

    private final PlayerRefreshJobRepository refreshJobRepository;

    private final Clock clock;

    private final Duration staleAfter;

    public PlayerDashboardServiceImpl(
            FrontendStatsService frontendStatsService,
            LeagueEntryRepository leagueEntryRepository,
            PlayerRepository playerRepository,
            PlayerRefreshJobRepository refreshJobRepository,
            Clock clock,
            @Value("${app.refresh.scheduled-min-age:6h}") Duration staleAfter
    ) {
        this.frontendStatsService = frontendStatsService;
        this.leagueEntryRepository = leagueEntryRepository;
        this.playerRepository = playerRepository;
        this.refreshJobRepository = refreshJobRepository;
        this.clock = clock;
        this.staleAfter = staleAfter;
    }

    @Override
    public PlayerDashboardDto getDashboard(String puuid) {
        List<PlayerRecentMatchDto> matches = frontendStatsService.getPlayerRecentMatches(puuid, 20);
        List<LeagueEntryEntity> rankEntities = leagueEntryRepository.findByPuuidOrderByQueueTypeAsc(puuid);
        List<PlayerChampionStatsDto> championPool = frontendStatsService.getPlayerChampions(puuid).
                stream().limit(10).toList();
        List<PlayerInsightDto> priorities = frontendStatsService.getPlayerInsights(puuid).
                stream().
                sorted(Comparator.comparing(
                                PlayerInsightDto::score,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        ).thenComparing(
                                PlayerInsightDto::createdAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )).
                limit(3).
                toList();
        PlayerRefreshStatusDto refresh = refreshJobRepository.
                findFirstByPuuidOrderByRequestedAtDesc(puuid).
                map(PlayerRefreshStatusDto::from).
                orElse(null);

        return new PlayerDashboardDto(
                frontendStatsService.getPlayerSummary(puuid),
                FORM_WINDOWS.stream().map(window -> recentForm(matches, window)).toList(),
                rankEntities.stream().map(this::rank).toList(),
                championPool,
                championPoolHealth(matches),
                priorities,
                freshness(puuid, matches, rankEntities),
                refresh
        );
    }

    private RecentFormDto recentForm(List<PlayerRecentMatchDto> allMatches, int window) {
        List<PlayerRecentMatchDto> matches = allMatches.stream().limit(window).toList();
        int wins = (int) matches.stream().filter(match -> Boolean.TRUE.equals(match.win())).count();
        double averageKda = matches.stream().mapToDouble(match ->
                (number(match.kills()) + number(match.assists())) / Math.max(1.0, number(match.deaths()))
        ).average().orElse(0.0);
        return new RecentFormDto(
                window,
                matches.size(),
                wins,
                matches.size() - wins,
                round(matches.isEmpty() ? 0.0 : wins * 100.0 / matches.size()),
                round(averageKda)
        );
    }

    private PlayerRankSummaryDto rank(LeagueEntryEntity rank) {
        int wins = number(rank.getWins());
        int losses = number(rank.getLosses());
        int games = wins + losses;
        return new PlayerRankSummaryDto(
                rank.getQueueType(),
                rank.getTier(),
                rank.getRankValue(),
                rank.getLeaguePoints(),
                rank.getWins(),
                rank.getLosses(),
                round(games == 0 ? 0.0 : wins * 100.0 / games),
                rank.getLastSyncedAt()
        );
    }

    private ChampionPoolHealthDto championPoolHealth(List<PlayerRecentMatchDto> matches) {
        int uniqueChampions = (int) matches.stream().map(PlayerRecentMatchDto::championId).
                filter(Objects::nonNull).distinct().count();
        String status;
        String message;
        if (uniqueChampions <= 3) {
            status = "FOCUSED";
            message = "Your recent champion pool is focused.";
        } else if (uniqueChampions <= 5) {
            status = "BALANCED";
            message = "Your recent champion pool has a healthy spread.";
        } else {
            status = "OVEREXTENDED";
            message = "Consider narrowing your champion pool for more consistent practice.";
        }
        return new ChampionPoolHealthDto(status, uniqueChampions, matches.size(), message);
    }

    private PlayerFreshnessDto freshness(
            String puuid,
            List<PlayerRecentMatchDto> matches,
            List<LeagueEntryEntity> ranks
    ) {
        Optional<OffsetDateTime> playerUpdated = playerRepository.findById(puuid).
                map(player -> player.getUpdatedAt());
        Optional<OffsetDateTime> rankUpdated = ranks.stream().map(LeagueEntryEntity::getLastSyncedAt).
                filter(Objects::nonNull).max(Comparator.naturalOrder());
        Optional<OffsetDateTime> matchUpdated = matches.stream().map(PlayerRecentMatchDto::gameCreationMs).
                filter(Objects::nonNull).max(Long::compareTo).
                map(value -> OffsetDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC));
        OffsetDateTime lastUpdated = List.of(playerUpdated, rankUpdated, matchUpdated).
                stream().flatMap(Optional::stream).max(Comparator.naturalOrder()).orElse(null);
        boolean stale = lastUpdated == null || lastUpdated.plus(staleAfter).isBefore(OffsetDateTime.now(clock));
        return new PlayerFreshnessDto(lastUpdated, stale, matches.size());
    }

    private int number(Integer value) {
        return value == null ? 0 : value;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
