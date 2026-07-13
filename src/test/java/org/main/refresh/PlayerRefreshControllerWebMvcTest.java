package org.main.refresh;

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
import org.main.refresh.dto.PlayerRefreshStatusDto;
import org.main.refresh.entity.RefreshSource;
import org.main.refresh.entity.RefreshState;
import org.main.refresh.service.PlayerRefreshCoordinator;
import org.main.refresh.web.PlayerRefreshController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlayerRefreshController.class)
@Import({SecurityConfig.class, AppSessionAuthenticationFilter.class})
class PlayerRefreshControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlayerRefreshCoordinator coordinator;

    @MockBean
    private SessionService sessionService;

    @Test
    void publicManualRefreshRequiresCsrfButNotAccount() throws Exception {
        when(coordinator.enqueue("puuid", RefreshSource.MANUAL)).thenReturn(statusDto());

        mockMvc.perform(post("/api/players/puuid/refresh")).
                andExpect(status().isForbidden());
        mockMvc.perform(post("/api/players/puuid/refresh").with(csrf())).
                andExpect(status().isAccepted()).
                andExpect(jsonPath("$.state").value("QUEUED"));
    }

    @Test
    void latestRefreshStatusRemainsPublic() throws Exception {
        when(coordinator.latest("puuid")).thenReturn(statusDto());

        mockMvc.perform(get("/api/players/puuid/refresh-status")).
                andExpect(status().isOk()).
                andExpect(jsonPath("$.puuid").value("puuid"));
    }

    @Test
    void cooldownReturnsTooManyRequestsWithRetryAfter() throws Exception {
        when(coordinator.enqueue("puuid", RefreshSource.MANUAL)).thenThrow(
                new PlayerRefreshCoordinator.RefreshCooldownException(java.time.Duration.ofSeconds(60))
        );

        mockMvc.perform(post("/api/players/puuid/refresh").with(csrf())).
                andExpect(status().isTooManyRequests()).
                andExpect(jsonPath("$.message").value("This profile was refreshed recently. Try again shortly."));
    }

    private PlayerRefreshStatusDto statusDto() {
        return new PlayerRefreshStatusDto(
                UUID.fromString("be88587f-04b1-49a9-a11a-89d76ddae08e"),
                "puuid",
                RefreshSource.MANUAL,
                RefreshState.QUEUED,
                OffsetDateTime.parse("2026-07-13T06:00:00Z"),
                null,
                null,
                null,
                null
        );
    }
}
