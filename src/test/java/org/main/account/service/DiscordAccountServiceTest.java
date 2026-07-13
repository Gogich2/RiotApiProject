package org.main.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.main.account.entity.AppUserEntity;
import org.main.account.entity.AppUserStatus;
import org.main.account.entity.OAuthIdentityEntity;
import org.main.account.repository.AppUserRepository;
import org.main.account.repository.OAuthIdentityRepository;
import org.main.account.security.AppPrincipal;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiscordAccountServiceTest {

    private static final UUID USER_ID = UUID.fromString("5ce095fb-cdce-47ae-b34c-ad7b47fcc6a8");

    private static final Instant INSTANT = Instant.parse("2026-07-13T04:00:00Z");

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private OAuthIdentityRepository identityRepository;

    private DiscordAccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = service(true);
        lenient().when(userRepository.save(any(AppUserEntity.class))).
                thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(identityRepository.save(any(OAuthIdentityEntity.class))).
                thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void repeatLoginUsesImmutableDiscordIdAfterUsernameChanges() {
        OAuthIdentityEntity identity = new OAuthIdentityEntity();
        identity.setUserId(USER_ID);
        when(identityRepository.findByProviderAndProviderSubjectId("discord", "123456789")).
                thenReturn(Optional.of(identity));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        Map<String, Object> attributes = profile();
        attributes.put("username", "renamed-user");

        AppPrincipal principal = accountService.authenticate(attributes);

        assertThat(principal.userId()).isEqualTo(USER_ID);
        assertThat(identity.getLastLoginAt()).isEqualTo(OffsetDateTime.ofInstant(INSTANT, ZoneOffset.UTC));
        verify(identityRepository).save(identity);
    }

    @Test
    void rejectsMissingOrUnverifiedEmail() {
        Map<String, Object> missingEmail = profile();
        missingEmail.remove("email");
        Map<String, Object> unverifiedEmail = profile();
        unverifiedEmail.put("verified", false);

        assertThatThrownBy(() -> accountService.authenticate(missingEmail)).
                isInstanceOf(DiscordAccountService.DiscordAccountException.class);
        assertThatThrownBy(() -> accountService.authenticate(unverifiedEmail)).
                isInstanceOf(DiscordAccountService.DiscordAccountException.class);
    }

    @Test
    void linksVerifiedEmailToExistingActiveUser() {
        when(identityRepository.findByProviderAndProviderSubjectId("discord", "123456789")).
                thenReturn(Optional.empty());
        when(userRepository.findByEmailNormalized("player@example.com")).
                thenReturn(Optional.of(activeUser()));

        AppPrincipal principal = accountService.authenticate(profile());

        ArgumentCaptor<OAuthIdentityEntity> captor = ArgumentCaptor.forClass(OAuthIdentityEntity.class);
        verify(identityRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(principal.userId()).isEqualTo(USER_ID);
    }

    @Test
    void createsActiveDiscordBackedUserWhenEmailIsNew() {
        when(identityRepository.findByProviderAndProviderSubjectId("discord", "123456789")).
                thenReturn(Optional.empty());
        when(userRepository.findByEmailNormalized("player@example.com")).thenReturn(Optional.empty());

        AppPrincipal principal = accountService.authenticate(profile());

        ArgumentCaptor<AppUserEntity> captor = ArgumentCaptor.forClass(AppUserEntity.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AppUserStatus.ACTIVE);
        assertThat(captor.getValue().getEmailVerifiedAt()).isEqualTo(
                OffsetDateTime.ofInstant(INSTANT, ZoneOffset.UTC)
        );
        assertThat(captor.getValue().getPasswordHash()).isNull();
        assertThat(principal.userId()).isEqualTo(captor.getValue().getId());
    }

    @Test
    void rejectsLoginWhenDiscordIsDisabled() {
        assertThatThrownBy(() -> service(false).authenticate(profile())).
                isInstanceOf(DiscordAccountService.DiscordAccountException.class);
    }

    private DiscordAccountService service(boolean enabled) {
        return new DiscordAccountService(
                userRepository,
                identityRepository,
                Clock.fixed(INSTANT, ZoneOffset.UTC),
                enabled
        );
    }

    private Map<String, Object> profile() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "123456789");
        attributes.put("email", "PLAYER@example.com");
        attributes.put("verified", true);
        attributes.put("global_name", "Player Name");
        attributes.put("username", "player-name");
        return attributes;
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
