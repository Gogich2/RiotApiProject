package org.main.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.main.account.entity.AppUserEntity;
import org.main.account.entity.AppUserStatus;
import org.main.account.entity.UserSessionEntity;
import org.main.account.repository.AppUserRepository;
import org.main.account.repository.UserSessionRepository;
import org.main.account.security.AppPrincipal;
import org.main.account.security.OpaqueTokenCodec;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    private static final Instant INSTANT = Instant.parse("2026-07-13T02:30:00Z");

    private static final UUID USER_ID = UUID.fromString("8c49679d-d4ca-4f28-a4ad-5eb660a8cb90");

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private AppUserRepository appUserRepository;

    private OpaqueTokenCodec tokenCodec;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(INSTANT, ZoneOffset.UTC);
        tokenCodec = new OpaqueTokenCodec();
        sessionService = new SessionService(
                userSessionRepository,
                appUserRepository,
                tokenCodec,
                clock,
                Duration.ofDays(30)
        );
        lenient().when(userSessionRepository.save(any(UserSessionEntity.class))).
                thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void storesOnlyHashWhenIssuingSession() {
        SessionIssue issue = sessionService.issue(USER_ID);
        ArgumentCaptor<UserSessionEntity> captor = ArgumentCaptor.forClass(UserSessionEntity.class);
        verify(userSessionRepository).save(captor.capture());

        UserSessionEntity stored = captor.getValue();
        assertThat(issue.rawToken()).hasSizeGreaterThanOrEqualTo(43);
        assertThat(stored.getTokenHash()).hasSize(64);
        assertThat(stored.getTokenHash()).isEqualTo(tokenCodec.hash(issue.rawToken()));
        assertThat(stored.getTokenHash()).isNotEqualTo(issue.rawToken());
        assertThat(stored.getExpiresAt()).isEqualTo(OffsetDateTime.ofInstant(
                INSTANT.plus(Duration.ofDays(30)), ZoneOffset.UTC));
    }

    @Test
    void resolvesActiveSessionToApplicationPrincipal() {
        UserSessionEntity session = new UserSessionEntity();
        session.setUserId(USER_ID);
        when(userSessionRepository.findActiveByTokenHash(any(), any())).thenReturn(Optional.of(session));
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(activeUser()));

        Optional<AppPrincipal> principal = sessionService.resolve("raw-token");

        assertThat(principal).contains(new AppPrincipal(USER_ID, "player@example.com", "Player"));
    }

    @Test
    void returnsEmptyWhenSessionIsExpiredOrUnknown() {
        when(userSessionRepository.findActiveByTokenHash(any(), any())).thenReturn(Optional.empty());

        assertThat(sessionService.resolve("expired-token")).isEmpty();
    }

    @Test
    void rotationRevokesOldTokenAndIssuesNewToken() {
        UserSessionEntity oldSession = new UserSessionEntity();
        oldSession.setUserId(USER_ID);
        when(userSessionRepository.findActiveByTokenHash(any(), any())).thenReturn(Optional.of(oldSession));

        SessionIssue issue = sessionService.rotate("old-token", USER_ID);

        verify(userSessionRepository).findActiveByTokenHash(tokenCodec.hash("old-token"),
                OffsetDateTime.ofInstant(INSTANT, ZoneOffset.UTC));
        assertThat(oldSession.getRevokedAt()).isEqualTo(OffsetDateTime.ofInstant(INSTANT, ZoneOffset.UTC));
        assertThat(issue.rawToken()).isNotEqualTo("old-token");
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
