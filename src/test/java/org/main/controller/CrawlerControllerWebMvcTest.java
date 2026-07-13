package org.main.controller;

import org.junit.jupiter.api.Test;
import org.main.account.security.AppSessionAuthenticationFilter;
import org.main.account.service.SessionService;
import org.main.config.SecurityConfig;
import org.main.dto.CrawlResultDto;
import org.main.persistence.repository.PlayerRepository;
import org.main.service.BalancedDatasetCrawlerService;
import org.main.service.CrawlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CrawlerController.class)
@Import({SecurityConfig.class, AppSessionAuthenticationFilter.class})
class CrawlerControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    CrawlerService crawlerService;

    @MockBean
    BalancedDatasetCrawlerService balancedDatasetCrawlerService;

    @MockBean
    PlayerRepository playerRepository;

    @MockBean
    SessionService sessionService;

    @Test
    void crawlByPuuidUsesDefaultLimit20() throws Exception {
        when(crawlerService.crawlPuuidEUW(eq("p"), eq(20))).thenReturn(mock(CrawlResultDto.class));

        mockMvc.perform(post("/api/crawl/euw/puuid/{puuid}", "p").with(csrf())).
                andExpect(status().isOk());

        verify(crawlerService).crawlPuuidEUW("p", 20);
        verifyNoMoreInteractions(crawlerService);
    }

    @Test
    void crawlByPuuidPassesProvidedLimit() throws Exception {
        when(crawlerService.crawlPuuidEUW(eq("p"), eq(7))).thenReturn(mock(CrawlResultDto.class));

        mockMvc.perform(post("/api/crawl/euw/puuid/{puuid}?limit=7", "p").with(csrf())).
                andExpect(status().isOk());

        verify(crawlerService).crawlPuuidEUW("p", 7);
        verifyNoMoreInteractions(crawlerService);
    }
}
