package org.main.account.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.main.account.service.SessionIssue;
import org.main.account.service.SessionService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.AuthenticationException;

@ExtendWith(MockitoExtension.class)
class DiscordAuthenticationSuccessHandlerTest {

    private static final UUID USER_ID = UUID.fromString("5ce095fb-cdce-47ae-b34c-ad7b47fcc6a8");

    @Mock
    private SessionService sessionService;

    private DiscordAuthenticationSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DiscordAuthenticationSuccessHandler(
                sessionService,
                "RIOT_STATS_SESSION",
                false,
                Duration.ofDays(30)
        );
    }

    @Test
    void issuesApplicationCookieClearsOauthStateAndRedirects() throws Exception {
        AppPrincipal appPrincipal = new AppPrincipal(USER_ID, "player@example.com", "Player");
        DiscordOAuth2UserService.DiscordOAuth2User oauthUser = mock(
                DiscordOAuth2UserService.DiscordOAuth2User.class
        );
        when(oauthUser.appPrincipal()).thenReturn(appPrincipal);
        when(sessionService.issue(USER_ID)).thenReturn(new SessionIssue(
                "raw-session-token",
                OffsetDateTime.parse("2026-08-12T04:00:00Z")
        ));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("oauth-state", "transient");
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, new TestingAuthenticationToken(oauthUser, null));

        verify(sessionService).issue(USER_ID);
        assertThat(response.getRedirectedUrl()).isEqualTo("/account.html?oauth=success");
        assertThat(response.getHeader("Set-Cookie")).contains(
                "RIOT_STATS_SESSION=raw-session-token",
                "HttpOnly",
                "SameSite=Lax"
        );
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void redirectsProviderFailureWithoutLeakingDetails() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(
                new MockHttpServletRequest(),
                response,
                mock(AuthenticationException.class)
        );

        assertThat(response.getRedirectedUrl()).isEqualTo("/account.html?oauth=error");
    }
}
