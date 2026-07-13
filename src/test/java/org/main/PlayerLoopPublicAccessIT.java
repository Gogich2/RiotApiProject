package org.main;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.main.account.security.AppPrincipal;
import org.main.account.security.AppSessionAuthenticationFilter;
import org.main.account.service.SavedProfileService;
import org.main.account.service.SessionService;
import org.main.account.web.SavedProfileController;
import org.main.config.SecurityConfig;
import org.main.controller.frontend.ChampionController;
import org.main.controller.frontend.MatchController;
import org.main.controller.frontend.PlayerController;
import org.main.persistence.repository.LeagueEntryRepository;
import org.main.persistence.repository.LeagueEntrySnapshotRepository;
import org.main.service.RankEnrichmentService;
import org.main.service.frontend.FrontendStatsService;
import org.main.service.frontend.PlayerDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({
        PlayerController.class,
        ChampionController.class,
        MatchController.class,
        SavedProfileController.class
})
@Import({SecurityConfig.class, AppSessionAuthenticationFilter.class})
class PlayerLoopPublicAccessIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FrontendStatsService frontendStatsService;

    @MockBean
    private PlayerDashboardService playerDashboardService;

    @MockBean
    private RankEnrichmentService rankEnrichmentService;

    @MockBean
    private LeagueEntryRepository leagueEntryRepository;

    @MockBean
    private LeagueEntrySnapshotRepository leagueEntrySnapshotRepository;

    @MockBean
    private SavedProfileService savedProfileService;

    @MockBean
    private SessionService sessionService;

    @Test
    void keepsPagesAndPlayerDataPublic() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/player.html").queryParam("puuid", "public-puuid")).
                andExpect(status().isOk());
        mockMvc.perform(get("/api/players/public-puuid/dashboard")).
                andExpect(status().isOk());
        mockMvc.perform(get("/api/champions/1")).
                andExpect(status().isOk());
        mockMvc.perform(get("/api/matches/EUW1_1/details")).
                andExpect(status().isOk());
    }

    @Test
    void rejectsAnonymousSavedProfileMutation() throws Exception {
        mockMvc.perform(post("/api/account/saved-profiles").
                        with(csrf()).
                        contentType(MediaType.APPLICATION_JSON).
                        content("""
                                {"puuid":"public-puuid","personalLabel":"Practice"}
                                """)).
                andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAuthenticatedMutationWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/account/saved-profiles").
                        with(authentication(appAuthentication())).
                        contentType(MediaType.APPLICATION_JSON).
                        content("""
                                {"puuid":"public-puuid","personalLabel":"Practice"}
                                """)).
                andExpect(status().isForbidden());
    }

    private UsernamePasswordAuthenticationToken appAuthentication() {
        AppPrincipal principal = new AppPrincipal(
                UUID.fromString("47c1e486-e990-4db5-a073-bf22049f0f1a"),
                "player@example.com",
                "Player"
        );
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
    }
}
