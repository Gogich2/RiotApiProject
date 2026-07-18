package org.main.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.main.client.RiotApiClient;
import org.main.dto.DataIntegrityReportDto;
import org.main.persistence.entity.LeagueEntryEntity;
import org.main.persistence.entity.PlayerEntity;
import org.main.persistence.repository.MatchRepository;
import org.main.persistence.repository.MatchTimelineEventRepository;
import org.main.persistence.repository.MatchTimelineFrameRepository;
import org.main.persistence.repository.MatchTimelineRawRepository;
import org.main.persistence.repository.PlayerRepository;
import org.springframework.data.domain.PageRequest;

class DataIntegrityServiceImplTest {

    @Test
    void repairsProfilesFromBoundedDatabaseCandidates() throws Exception {
        TestContext context = context();
        PlayerEntity player = player("profile-puuid");
        when(context.playerRepository.findPlayersMissingProfiles(PageRequest.of(0, 25))).
                thenReturn(List.of(player));
        when(context.riotApiClient.getSummonerByPuuidEuw("profile-puuid")).
                thenReturn(new ObjectMapper().readTree("{\"profileIconId\":12,\"summonerLevel\":99}"));

        context.service.repairMissingPlayerProfiles(25);

        verify(context.playerRepository).findPlayersMissingProfiles(PageRequest.of(0, 25));
        verify(context.playerRepository, never()).findAll();
        verify(context.playerRepository).save(player);
        assertThat(player.getProfileIconId()).isEqualTo(12);
    }

    @Test
    void repairsRanksFromBoundedDatabaseCandidates() {
        TestContext context = context();
        PlayerEntity player = player("rank-puuid");
        when(context.playerRepository.findPlayersMissingRanks(PageRequest.of(0, 10))).
                thenReturn(List.of(player));
        when(context.rankEnrichmentService.enrichRanksForPuuidEuw("rank-puuid")).
                thenReturn(new RankEnrichmentResult(List.of(new LeagueEntryEntity()), true));

        context.service.repairMissingRanks(10);

        verify(context.playerRepository).findPlayersMissingRanks(PageRequest.of(0, 10));
        verify(context.playerRepository, never()).findAll();
        verify(context.rankEnrichmentService, never()).hasRankData("rank-puuid");
    }

    @Test
    void repairsStoredTimelineProjectionsWithoutFetchingTimelines() throws Exception {
        MatchRepository matchRepository = mock(MatchRepository.class);
        MatchTimelineRawRepository rawRepository = mock(MatchTimelineRawRepository.class);
        MatchTimelineFrameRepository frameRepository = mock(MatchTimelineFrameRepository.class);
        MatchTimelineEventRepository eventRepository = mock(MatchTimelineEventRepository.class);
        TimelineIngestService timelineIngestService = mock(TimelineIngestService.class);
        RiotApiClient riotApiClient = mock(RiotApiClient.class);
        DataIntegrityServiceImpl service = new DataIntegrityServiceImpl(
                matchRepository,
                rawRepository,
                frameRepository,
                eventRepository,
                timelineIngestService,
                mock(PlayerRepository.class),
                mock(RankEnrichmentService.class),
                riotApiClient
        );
        when(matchRepository.findTimelineRawIdsWithoutFrames()).
                thenReturn(List.of("frames-missing"));
        when(matchRepository.findTimelineRawIdsWithoutEvents()).
                thenReturn(List.of("events-missing"));

        DataIntegrityReportDto report = service.repairStoredTimelineData(50);

        verify(timelineIngestService).repairTimelineFromRaw("frames-missing");
        verify(timelineIngestService).repairTimelineFromRaw("events-missing");
        verify(timelineIngestService, never()).ingestTimelineIfMissing(org.mockito.ArgumentMatchers.any());
        verify(matchRepository, never()).findMatchIdsWithoutTimelineRaw();
        verifyNoInteractions(riotApiClient);
        assertThat(report).isNotNull();
    }

    private TestContext context() {
        MatchRepository matchRepository = mock(MatchRepository.class);
        MatchTimelineRawRepository rawRepository = mock(MatchTimelineRawRepository.class);
        MatchTimelineFrameRepository frameRepository = mock(MatchTimelineFrameRepository.class);
        MatchTimelineEventRepository eventRepository = mock(MatchTimelineEventRepository.class);
        TimelineIngestService timelineIngestService = mock(TimelineIngestService.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        RankEnrichmentService rankEnrichmentService = mock(RankEnrichmentService.class);
        RiotApiClient riotApiClient = mock(RiotApiClient.class);
        DataIntegrityServiceImpl service = new DataIntegrityServiceImpl(
                matchRepository,
                rawRepository,
                frameRepository,
                eventRepository,
                timelineIngestService,
                playerRepository,
                rankEnrichmentService,
                riotApiClient
        );
        return new TestContext(service, playerRepository, rankEnrichmentService, riotApiClient);
    }

    private PlayerEntity player(String puuid) {
        PlayerEntity player = new PlayerEntity();
        player.setPuuid(puuid);
        return player;
    }

    private record TestContext(
            DataIntegrityServiceImpl service,
            PlayerRepository playerRepository,
            RankEnrichmentService rankEnrichmentService,
            RiotApiClient riotApiClient
    ) {
    }
}
