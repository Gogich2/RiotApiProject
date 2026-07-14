package org.main.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.main.client.RiotApiClient;
import org.main.dto.DataIntegrityReportDto;
import org.main.persistence.repository.MatchRepository;
import org.main.persistence.repository.MatchTimelineEventRepository;
import org.main.persistence.repository.MatchTimelineFrameRepository;
import org.main.persistence.repository.MatchTimelineRawRepository;
import org.main.persistence.repository.PlayerRepository;

class DataIntegrityServiceImplTest {

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
}
