package org.main.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.main.client.RiotApiClient;
import org.main.persistence.repository.MatchRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrawlerServiceImplPaginationTest {

    @Test
    void crawlPuuidEUW_paginates_untilLimit() {
        RiotApiClient api = mock(RiotApiClient.class);
        MatchRepository repo = mock(MatchRepository.class);

        CrawlerServiceImpl service = new CrawlerServiceImpl(api, repo);

        List<String> page1 = java.util.stream.IntStream.range(0, 20)
                .mapToObj(i -> "m" + i).toList();
        List<String> page2 = java.util.stream.IntStream.range(20, 25)
                .mapToObj(i -> "m" + i).toList();

        when(api.getMatchIdsByPuuidEurope(eq("puuid"), eq(0), eq(20))).thenReturn(page1);
        when(api.getMatchIdsByPuuidEurope(eq("puuid"), eq(20), eq(5))).thenReturn(page2);

        when(repo.existsById(anyString())).thenReturn(false);
        when(api.getMatchByIdEurope(anyString())).thenReturn(mock(JsonNode.class));

        var dto = service.crawlPuuidEUW("puuid", 25);

        assertEquals(25, dto.requestedLimit());
        assertEquals(25, dto.savedNewMatches());
        assertEquals(25, dto.savedMatchIds().size());

        verify(api).getMatchIdsByPuuidEurope("puuid", 0, 20);
        verify(api).getMatchIdsByPuuidEurope("puuid", 20, 5);

        verify(repo, times(25)).save(any());
    }
}
