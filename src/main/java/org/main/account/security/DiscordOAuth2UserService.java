package org.main.account.security;

import java.util.Collection;
import java.util.Map;
import org.main.account.service.DiscordAccountService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class DiscordOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    private final DiscordAccountService accountService;

    public DiscordOAuth2UserService(DiscordAccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = delegate.loadUser(userRequest);
        try {
            AppPrincipal principal = accountService.authenticate(oauthUser.getAttributes());
            return new DiscordOAuth2User(oauthUser, principal);
        } catch (DiscordAccountService.DiscordAccountException exception) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("discord_account_error"),
                    "Discord sign-in could not be completed.",
                    exception
            );
        }
    }

    public static class DiscordOAuth2User implements OAuth2User {

        private final OAuth2User delegate;

        private final AppPrincipal appPrincipal;

        public DiscordOAuth2User(OAuth2User delegate, AppPrincipal appPrincipal) {
            this.delegate = delegate;
            this.appPrincipal = appPrincipal;
        }

        public AppPrincipal appPrincipal() {
            return appPrincipal;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return delegate.getAttributes();
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return delegate.getAuthorities();
        }

        @Override
        public String getName() {
            return delegate.getName();
        }
    }
}
