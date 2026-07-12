package org.main.config;

import java.time.Clock;
import org.main.account.security.AppSessionAuthenticationFilter;
import org.main.account.security.DiscordAuthenticationSuccessHandler;
import org.main.account.security.DiscordOAuth2UserService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AppSessionAuthenticationFilter sessionFilter,
            ObjectProvider<DiscordOAuth2UserService> oauthUserServiceProvider,
            ObjectProvider<DiscordAuthenticationSuccessHandler> oauthHandlerProvider
    ) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        http.authorizeHttpRequests(authorize -> authorize.
                        requestMatchers("/api/account/**").authenticated().
                        requestMatchers("/", "/*.html", "/css/**", "/js/**", "/img/**").permitAll().
                        requestMatchers("/api/auth/**", "/oauth2/**", "/login/oauth2/**").permitAll().
                        requestMatchers(HttpMethod.GET, "/api/**").permitAll().
                        anyRequest().permitAll()).
                csrf(csrf -> csrf.csrfTokenRepository(csrfRepository)).
                exceptionHandling(exceptions -> exceptions.
                        authenticationEntryPoint((request, response, exception) ->
                                response.sendError(HttpStatus.UNAUTHORIZED.value())).
                        accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpStatus.FORBIDDEN.value()))).
                addFilterBefore(sessionFilter, AnonymousAuthenticationFilter.class);
        DiscordOAuth2UserService oauthUserService = oauthUserServiceProvider.getIfAvailable();
        DiscordAuthenticationSuccessHandler oauthHandler = oauthHandlerProvider.getIfAvailable();
        if (oauthUserService != null && oauthHandler != null) {
            http.oauth2Login(oauth -> oauth.
                    userInfoEndpoint(userInfo -> userInfo.userService(oauthUserService)).
                    successHandler(oauthHandler).
                    failureHandler(oauthHandler));
        }
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
