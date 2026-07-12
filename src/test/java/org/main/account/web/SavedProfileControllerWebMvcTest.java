package org.main.account.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.main.account.dto.SavedProfileDto;
import org.main.account.security.AppPrincipal;
import org.main.account.security.AppSessionAuthenticationFilter;
import org.main.account.service.SavedProfileService;
import org.main.account.service.SessionService;
import org.main.config.SecurityConfig;
import org.main.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SavedProfileController.class)
@Import({SecurityConfig.class, AppSessionAuthenticationFilter.class})
class SavedProfileControllerWebMvcTest {

    private static final UUID USER_ID = UUID.fromString("23da8d39-fc2e-460c-b6dc-28ab0821cc82");

    private static final UUID SAVED_ID = UUID.fromString("ab438de5-4983-4182-9e67-ae52187cd38e");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SavedProfileService savedProfileService;

    @MockBean
    private SessionService sessionService;

    @Test
    void anonymousMutationReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/account/saved-profiles").
                        with(csrf()).
                        contentType(MediaType.APPLICATION_JSON).
                        content("""
                                {"puuid":"known-puuid","personalLabel":"Main"}
                                """)).
                andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedMutationWithoutCsrfReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/account/saved-profiles").
                        with(authentication(appAuthentication())).
                        contentType(MediaType.APPLICATION_JSON).
                        content("""
                                {"puuid":"known-puuid","personalLabel":"Main"}
                                """)).
                andExpect(status().isForbidden());
    }

    @Test
    void authenticatedSaveReturnsCreatedBookmark() throws Exception {
        when(savedProfileService.save(USER_ID, "known-puuid", "Main")).thenReturn(savedDto());

        mockMvc.perform(post("/api/account/saved-profiles").
                        with(authentication(appAuthentication())).
                        with(csrf()).
                        contentType(MediaType.APPLICATION_JSON).
                        content("""
                                {"puuid":"known-puuid","personalLabel":"Main"}
                                """)).
                andExpect(status().isCreated()).
                andExpect(jsonPath("$.puuid").value("known-puuid")).
                andExpect(jsonPath("$.personalLabel").value("Main"));
    }

    @Test
    void anotherUsersBookmarkLooksNotFound() throws Exception {
        when(savedProfileService.update(USER_ID, SAVED_ID, "Other", false)).
                thenThrow(new NotFoundException("Saved profile not found"));

        mockMvc.perform(patch("/api/account/saved-profiles/{id}", SAVED_ID).
                        with(authentication(appAuthentication())).
                        with(csrf()).
                        contentType(MediaType.APPLICATION_JSON).
                        content("""
                                {"personalLabel":"Other","isDefault":false}
                                """)).
                andExpect(status().isNotFound());
    }

    @Test
    void authenticatedDeleteUsesPrincipalUserId() throws Exception {
        mockMvc.perform(delete("/api/account/saved-profiles/{id}", SAVED_ID).
                        with(authentication(appAuthentication())).
                        with(csrf())).
                andExpect(status().isNoContent());

        verify(savedProfileService).delete(USER_ID, SAVED_ID);
    }

    private UsernamePasswordAuthenticationToken appAuthentication() {
        AppPrincipal principal = new AppPrincipal(USER_ID, "player@example.com", "Player");
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
    }

    private SavedProfileDto savedDto() {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-13T05:00:00Z");
        return new SavedProfileDto(
                SAVED_ID,
                "known-puuid",
                "Player",
                "EUW",
                123,
                "Main",
                false,
                now,
                now
        );
    }
}
