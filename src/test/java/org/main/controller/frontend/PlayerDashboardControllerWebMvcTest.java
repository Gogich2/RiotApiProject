package org.main.controller.frontend;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.main.account.security.AppSessionAuthenticationFilter;
import org.main.account.service.SessionService;
import org.main.config.SecurityConfig;
import org.main.dto.frontend.RiotIdResolveResponse;
import org.main.refresh.dto.PlayerRefreshStatusDto;
import org.main.refresh.entity.RefreshSource;
import org.main.refresh.entity.RefreshState;
import org.main.persistence.repository.LeagueEntryRepository;
import org.main.persistence.repository.LeagueEntrySnapshotRepository;
import org.main.service.RankEnrichmentService;
import org.main.service.frontend.FrontendStatsService;
import org.main.service.frontend.PlayerDashboardService;
import org.main.service.frontend.RiotIdResolveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({PlayerController.class, RiotIdResolveController.class})
@Import({SecurityConfig.class, AppSessionAuthenticationFilter.class})
class PlayerDashboardControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlayerDashboardService dashboardService;

    @MockBean
    private FrontendStatsService frontendStatsService;

    @MockBean
    private RankEnrichmentService rankEnrichmentService;

    @MockBean
    private LeagueEntryRepository leagueEntryRepository;

    @MockBean
    private LeagueEntrySnapshotRepository leagueEntrySnapshotRepository;

    @MockBean
    private RiotIdResolveService resolveService;

    @MockBean
    private SessionService sessionService;

    @Test
    void dashboardReadRemainsPublic() throws Exception {
        mockMvc.perform(get("/api/players/puuid/dashboard")).
                andExpect(status().isOk());
    }

    @Test
    void publicRiotIdResolveRequiresCsrf() throws Exception {
        when(resolveService.resolve("Player", "EUW")).thenReturn(resolveResponse());

        mockMvc.perform(post("/api/players/resolve").
                        contentType(MediaType.APPLICATION_JSON).
                        content("""
                                {"gameName":"Player","tagLine":"EUW"}
                                """)).
                andExpect(status().isForbidden());
        mockMvc.perform(post("/api/players/resolve").
                        with(csrf()).
                        contentType(MediaType.APPLICATION_JSON).
                        content("""
                                {"gameName":"Player","tagLine":"EUW"}
                                """)).
                andExpect(status().isOk()).
                andExpect(jsonPath("$.puuid").value("new-puuid"));
    }

    private RiotIdResolveResponse resolveResponse() {
        PlayerRefreshStatusDto refresh = new PlayerRefreshStatusDto(
                UUID.fromString("f85fc970-d1fa-48fd-9995-af203109f350"),
                "new-puuid",
                RefreshSource.RESOLVE,
                RefreshState.QUEUED,
                OffsetDateTime.parse("2026-07-13T06:00:00Z"),
                null,
                null,
                null,
                null
        );
        return new RiotIdResolveResponse("new-puuid", "Player", "EUW", refresh);
    }
}
