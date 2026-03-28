package org.main.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.main.client.RiotApiClient;
import org.main.dto.CrawlResultDto;
import org.main.persistence.entity.MatchEntity;
import org.main.persistence.repository.MatchRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class Testing {

    @Mock
    RiotApiClient riotApiClient;

    @Mock
    MatchRepository matchRepository;

    @InjectMocks
    CrawlerServiceImpl crawlerService;

    private final ObjectMapper mapper = new ObjectMapper();


    @Test
    void clampLimitShouldClampCorrectly() {
        assertEquals(20, CrawlerServiceImpl.clampLimit(0));
        assertEquals(20, CrawlerServiceImpl.clampLimit(-5));
        assertEquals(1,  CrawlerServiceImpl.clampLimit(1));
        assertEquals(50, CrawlerServiceImpl.clampLimit(50));
        assertEquals(100, CrawlerServiceImpl.clampLimit(999));
    }


    @Test
    void crawlPuuidEUWShouldSaveOnlyNewMatches() throws Exception {
        String puuid = "test-puuid";

        when(riotApiClient.getMatchIdsByPuuidEurope(puuid, 0, 2)).
                thenReturn(List.of("EUW1_123", "EUW1_456"));

        when(matchRepository.existsById("EUW1_123")).thenReturn(true);
        when(matchRepository.existsById("EUW1_456")).thenReturn(false);

        JsonNode matchJson = mapper.readTree("{\"metadata\": {\"matchId\": \"EUW1_456\"}}");
        when(riotApiClient.getMatchByIdEurope("EUW1_456")).thenReturn(matchJson);

        CrawlResultDto result = crawlerService.crawlPuuidEUW(puuid, 2);

        verify(matchRepository, times(1)).save(any(MatchEntity.class));

        assertEquals(1, extractSavedCount(result));
        assertEquals(1, extractSavedMatchIds(result).size());
        assertTrue(extractSavedMatchIds(result).contains("EUW1_456"));
    }


    @Test
    void crawlPuuidEUWShouldPaginateUntilLimitReached() throws Exception {
        String puuid = "test-puuid";
        int limit = 25;

        // First page returns 20 ids, second page returns 5 ids
        List<String> page1 = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            page1.add("EUW1_" + i);
        };

        List<String> page2 = new ArrayList<>();
        for (int i = 21; i <= 25; i++) {
            page2.add("EUW1_" + i);
        };

        when(riotApiClient.getMatchIdsByPuuidEurope(puuid, 0, 20)).thenReturn(page1);
        when(riotApiClient.getMatchIdsByPuuidEurope(puuid, 20, 5)).thenReturn(page2);

        // All are new
        when(matchRepository.existsById(anyString())).thenReturn(false);

        JsonNode matchJson = mapper.readTree("{\"metadata\": {\"ok\": true}}");
        when(riotApiClient.getMatchByIdEurope(anyString())).thenReturn(matchJson);

        CrawlResultDto result = crawlerService.crawlPuuidEUW(puuid, limit);

        // Should request match list twice
        verify(riotApiClient, times(1)).getMatchIdsByPuuidEurope(puuid, 0, 20);
        verify(riotApiClient, times(1)).getMatchIdsByPuuidEurope(puuid, 20, 5);

        // Should save 25 matches
        verify(matchRepository, times(25)).save(any(MatchEntity.class));

        assertEquals(25, extractSavedCount(result));
        assertEquals(25, extractSavedMatchIds(result).size());
    }

    // ------------------------------------------------------------
    // 4) crawlRiotIdEUW: RiotID > puuid > crawl matches > save
    // ------------------------------------------------------------
    @Test
    void crawlRiotIdEUWShouldResolvePuuidAndSaveMatches() throws Exception {
        String gameName = "acoomer";
        String tagLine = "EUW";
        String puuid = "resolved-puuid";

        // RiotID > account JSON > puuid
        JsonNode accountJson = mapper.readTree("{\"puuid\":\"" + puuid + "\"}");
        when(riotApiClient.getAccountByRiotIdEurope(gameName, tagLine)).thenReturn(accountJson);

        // matchlist for puuid
        when(riotApiClient.getMatchIdsByPuuidEurope(puuid, 0, 3)).
                thenReturn(List.of("EUW1_1", "EUW1_2", "EUW1_3"));

        when(matchRepository.existsById(anyString())).thenReturn(false);

        JsonNode matchJson = mapper.readTree("{\"metadata\":{}}");
        when(riotApiClient.getMatchByIdEurope(anyString())).thenReturn(matchJson);

        CrawlResultDto result = crawlerService.crawlRiotIdEUW(gameName, tagLine, 3);

        // Must call riot-id endpoint
        verify(riotApiClient, times(1)).getAccountByRiotIdEurope(gameName, tagLine);

        // Must save 3
        verify(matchRepository, times(3)).save(any(MatchEntity.class));

        assertEquals(3, extractSavedCount(result));
        assertEquals(3, extractSavedMatchIds(result).size());
    }

    // ----------------------------------------
    // 5) input validation requirements (errors)
    // ----------------------------------------
    @Test
    void crawlPuuidEUWShouldThrowWhenPuuidIsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> crawlerService.crawlPuuidEUW("   ", 5));
    }

    @Test
    void crawlRiotIdEUWShouldThrowWhenRiotIdMissing() {
        assertThrows(IllegalArgumentException.class,
                () -> crawlerService.crawlRiotIdEUW("acoomer", "", 5));
        assertThrows(IllegalArgumentException.class,
                () -> crawlerService.crawlRiotIdEUW("", "EUW", 5));
    }

    private static int extractSavedCount(CrawlResultDto dto) {
        Object val = invokeFirstExisting(dto,
                "savedNewMatches", "getSavedNewMatches",
                "savedCount", "getSavedCount",
                "saved", "getSaved",
                "savedMatches", "getSavedMatches"
        );
        if (val instanceof Integer i) {
            return i;
        }
        if (val instanceof Number n) {
            return n.intValue();
        }
        // fallback: if we can't find count, derive from ids list
        return extractSavedMatchIds(dto).size();
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractSavedMatchIds(CrawlResultDto dto) {
        Object val = invokeFirstExisting(dto,
                "savedMatchIds", "getSavedMatchIds",
                "savedMatchIdList", "getSavedMatchIdList",
                "matchIds", "getMatchIds",
                "savedIds", "getSavedIds"
        );
        if (val == null) {
            return List.of();
        }
        if (val instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    private static Object invokeFirstExisting(Object target, String... methodNames) {
        for (String name : methodNames) {
            try {
                Method m = target.getClass().getMethod(name);
                return m.invoke(target);
            } catch (NoSuchMethodException ignored) {
                // try next
            } catch (Exception e) {
                throw new RuntimeException("Failed invoking method: " + name, e);
            }
        }
        return null;
    }
}
