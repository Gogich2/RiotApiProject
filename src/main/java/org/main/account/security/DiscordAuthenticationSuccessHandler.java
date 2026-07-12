package org.main.account.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Duration;
import org.main.account.service.SessionIssue;
import org.main.account.service.SessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class DiscordAuthenticationSuccessHandler
        implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private final SessionService sessionService;

    private final String cookieName;

    private final boolean secureCookie;

    private final Duration sessionDuration;

    public DiscordAuthenticationSuccessHandler(
            SessionService sessionService,
            @Value("${app.auth.session-cookie-name:RIOT_STATS_SESSION}") String cookieName,
            @Value("${app.auth.secure-cookie:false}") boolean secureCookie,
            @Value("${app.auth.session-duration:30d}") Duration sessionDuration
    ) {
        this.sessionService = sessionService;
        this.cookieName = cookieName;
        this.secureCookie = secureCookie;
        this.sessionDuration = sessionDuration;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        if (!(authentication.getPrincipal() instanceof DiscordOAuth2UserService.DiscordOAuth2User oauthUser)) {
            clearSession(request);
            response.sendRedirect("/account.html?oauth=error");
            return;
        }
        AppPrincipal principal = oauthUser.appPrincipal();
        SessionIssue session = sessionService.issue(principal.userId());
        ResponseCookie cookie = ResponseCookie.from(cookieName, session.rawToken()).
                httpOnly(true).
                secure(secureCookie).
                sameSite("Lax").
                path("/").
                maxAge(sessionDuration).
                build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        clearSession(request);
        response.sendRedirect("/account.html?oauth=success");
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        clearSession(request);
        response.sendRedirect("/account.html?oauth=error");
    }

    private void clearSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
