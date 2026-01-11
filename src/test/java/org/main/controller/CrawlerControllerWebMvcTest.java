package org.main.controller;

import org.junit.jupiter.api.Test;
import org.main.dto.CrawlResultDto;
import org.main.service.CrawlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CrawlerController.class)
class CrawlerControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    CrawlerService crawlerService;

    @Test
    void crawlByPuuid_usesDefaultLimit20() throws Exception {
        when(crawlerService.crawlPuuidEUW(eq("p"), eq(20))).thenReturn(mock(CrawlResultDto.class));

        mockMvc.perform(post("/api/crawl/euw/puuid/{puuid}", "p"))
                .andExpect(status().isOk());

        verify(crawlerService).crawlPuuidEUW("p", 20);
        verifyNoMoreInteractions(crawlerService);
    }

    @Test
    void crawlByPuuid_passesProvidedLimit() throws Exception {
        when(crawlerService.crawlPuuidEUW(eq("p"), eq(7))).thenReturn(mock(CrawlResultDto.class));

        mockMvc.perform(post("/api/crawl/euw/puuid/{puuid}?limit=7", "p"))
                .andExpect(status().isOk());

        verify(crawlerService).crawlPuuidEUW("p", 7);
        verifyNoMoreInteractions(crawlerService);
    }
}
