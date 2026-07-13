package org.main.account.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.main.account.entity.AppUserEntity;
import org.main.account.entity.AppUserStatus;
import org.main.account.security.AppPrincipal;
import org.main.account.security.AppSessionAuthenticationFilter;
import org.main.account.security.AuthRateLimiter;
import org.main.account.service.PasswordAuthService;
import org.main.account.service.SessionIssue;
import org.main.account.service.SessionService;
import org.main.config.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, AppSessionAuthenticationFilter.class})
class AuthControllerWebMvcTest {

    private static final UUID USER_ID = UUID.fromString("1cbe98c1-5956-4656-a03e-97e851a00bb0");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PasswordAuthService authService;

    @MockBean
    private SessionService sessionService;

    @MockBean
    private AuthRateLimiter rateLimiter;

    @Test
    void validatesRegistrationBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/auth/register").
                        with(csrf()).
                        contentType(MediaType.APPLICATION_JSON).
                        content("""
                                {"email":"not-an-email","password":"short","displayName":""}
                                """)).
                andExpect(status().isBadRequest());

        verify(authService, never()).register(anyString(), anyString(), anyString());
    }

    @Test
    void registersValidAccount() throws Exception {
        mockMvc.perform(post("/api/auth/register").
                        with(csrf()).
                        contentType(MediaType.APPLICATION_JSON).
                        content("""
                                {
                                  "email":"player@example.com",
                                  "password":"long-password",
                                  "displayName":"Player"
                                }
                                """)).
                andExpect(status().isCreated());

        verify(rateLimiter).check("127.0.0.1", "register");
        verify(authService).register("player@example.com", "long-password", "Player");
    }

    @Test
    void loginIssuesHardenedApplicationCookie() throws Exception {
        when(authService.login("player@example.com", "long-password")).thenReturn(activeUser());
        when(sessionService.issue(USER_ID)).thenReturn(new SessionIssue(
                "raw-session-token",
                OffsetDateTime.parse("2026-08-12T03:00:00Z")
        ));

        mockMvc.perform(post("/api/auth/login").
                        with(csrf()).
                        contentType(MediaType.APPLICATION_JSON).
                        content("""
                                {"email":"player@example.com","password":"long-password"}
                                """)).
                andExpect(status().isOk()).
                andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("RIOT_STATS_SESSION=raw-session-token"),
                        org.hamcrest.Matchers.containsString("Path=/"),
                        org.hamcrest.Matchers.containsString("Max-Age=2592000"),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("SameSite=Lax")
                ))).
                andExpect(jsonPath("$.authenticated").value(true)).
                andExpect(jsonPath("$.user.email").value("player@example.com"));
    }

    @Test
    void returnsAnonymousAndAuthenticatedCurrentUserStates() throws Exception {
        mockMvc.perform(get("/api/auth/me")).
                andExpect(status().isOk()).
                andExpect(jsonPath("$.authenticated").value(false)).
                andExpect(jsonPath("$.user").doesNotExist());

        mockMvc.perform(get("/api/auth/me").with(authentication(appAuthentication()))).
                andExpect(status().isOk()).
                andExpect(jsonPath("$.authenticated").value(true)).
                andExpect(jsonPath("$.user.displayName").value("Player"));
    }

    @Test
    void resetRequestAlwaysReturnsAccepted() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/request").
                        with(csrf()).
                        contentType(MediaType.APPLICATION_JSON).
                        content("""
                                {"email":"missing@example.com"}
                                """)).
                andExpect(status().isAccepted());

        verify(authService).requestPasswordReset("missing@example.com");
    }

    @Test
    void verifiesEmailAndConfirmsPasswordReset() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email").
                        with(csrf()).
                        contentType(MediaType.APPLICATION_JSON).
                        content("""
                                {"token":"verification-token"}
                                """)).
                andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/password-reset/confirm").
                        with(csrf()).
                        contentType(MediaType.APPLICATION_JSON).
                        content("""
                                {"token":"reset-token","newPassword":"replacement-password"}
                                """)).
                andExpect(status().isNoContent());

        verify(authService).verifyEmail("verification-token");
        verify(authService).resetPassword("reset-token", "replacement-password");
    }

    @Test
    void mapsRateLimitToSafeTooManyRequestsResponse() throws Exception {
        org.mockito.Mockito.doThrow(new AuthRateLimiter.AuthRateLimitException(java.time.Duration.ofMinutes(3))).
                when(rateLimiter).check("127.0.0.1", "login");

        mockMvc.perform(post("/api/auth/login").
                        with(csrf()).
                        contentType(MediaType.APPLICATION_JSON).
                        content("""
                                {"email":"player@example.com","password":"long-password"}
                                """)).
                andExpect(status().isTooManyRequests()).
                andExpect(jsonPath("$.message").value(
                        "Too many authentication attempts. Please try again later."
                ));
    }

    @Test
    void logoutRevokesAndClearsCurrentCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout").
                        with(csrf()).
                        cookie(new Cookie("RIOT_STATS_SESSION", "raw-session-token"))).
                andExpect(status().isNoContent()).
                andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

        verify(sessionService).revoke("raw-session-token");
    }

    private UsernamePasswordAuthenticationToken appAuthentication() {
        AppPrincipal principal = new AppPrincipal(USER_ID, "player@example.com", "Player");
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
    }

    private AppUserEntity activeUser() {
        AppUserEntity user = new AppUserEntity();
        user.setId(USER_ID);
        user.setEmailNormalized("player@example.com");
        user.setDisplayName("Player");
        user.setStatus(AppUserStatus.ACTIVE);
        return user;
    }
}
