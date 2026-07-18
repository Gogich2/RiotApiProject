package org.main.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.main.client.RiotApiClient;
import org.main.persistence.entity.MatchTimelineEventEntity;
import org.main.persistence.entity.MatchTimelineFrameEntity;
import org.main.persistence.entity.MatchTimelineRawEntity;
import org.main.persistence.repository.MatchTimelineEventRepository;
import org.main.persistence.repository.MatchTimelineFrameRepository;
import org.main.persistence.repository.MatchTimelineRawRepository;
import org.mockito.ArgumentCaptor;

class TimelineIngestServiceImplTest {

    @Test
    void repairsFramesAndEventsInTwoBatches() {
        MatchTimelineRawRepository rawRepository = mock(MatchTimelineRawRepository.class);
        MatchTimelineFrameRepository frameRepository = mock(MatchTimelineFrameRepository.class);
        MatchTimelineEventRepository eventRepository = mock(MatchTimelineEventRepository.class);
        MatchTimelineRawEntity raw = new MatchTimelineRawEntity();
        raw.setRawTimelineJson("""
                {"info":{"frames":[
                  {"timestamp":0,"events":[{"timestamp":1,"type":"A"}]},
                  {"timestamp":60000,"events":[
                    {"timestamp":2,"type":"B"},
                    {"timestamp":3,"type":"C"}
                  ]}
                ]}}
                """);
        when(rawRepository.findById("EUW1_1")).thenReturn(Optional.of(raw));
        TimelineIngestServiceImpl service = new TimelineIngestServiceImpl(
                mock(RiotApiClient.class),
                rawRepository,
                frameRepository,
                eventRepository,
                new ObjectMapper(),
                mock(IngestLogService.class)
        );

        service.repairTimelineFromRaw("EUW1_1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<MatchTimelineFrameEntity>> frames = ArgumentCaptor.forClass(Iterable.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<MatchTimelineEventEntity>> events = ArgumentCaptor.forClass(Iterable.class);
        verify(frameRepository, times(1)).saveAll(frames.capture());
        verify(eventRepository, times(1)).saveAll(events.capture());
        assertThat(frames.getValue()).asList().hasSize(2);
        assertThat(events.getValue()).asList().hasSize(3);
        verify(frameRepository, never()).save(any());
    }
}
