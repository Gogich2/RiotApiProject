package org.main.account.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.main.account.service.SessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

@Component
public class AppSessionAuthenticationFilter extends OncePerRequestFilter {

    private final SessionService sessionService;

    private final String cookieName;

    public AppSessionAuthenticationFilter(
            SessionService sessionService,
            @Value("${app.auth.session-cookie-name:RIOT_STATS_SESSION}") String cookieName
    ) {
        this.sessionService = sessionService;
        this.cookieName = cookieName;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            Cookie cookie = WebUtils.getCookie(request, cookieName);
            if (cookie != null) {
                sessionService.resolve(cookie.getValue()).ifPresent(principal ->
                        SecurityContextHolder.getContext().setAuthentication(
                                UsernamePasswordAuthenticationToken.authenticated(
                                        principal,
                                        null,
                                        List.of()
                                )
                        ));
            }
        }
        filterChain.doFilter(request, response);
    }
}
