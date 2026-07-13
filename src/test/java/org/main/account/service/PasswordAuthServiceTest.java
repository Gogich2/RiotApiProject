package org.main.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.main.account.entity.AccountTokenType;
import org.main.account.entity.AppUserEntity;
import org.main.account.entity.AppUserStatus;
import org.main.account.mail.AccountMailService;
import org.main.account.repository.AppUserRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordAuthServiceTest {

    private static final UUID USER_ID = UUID.fromString("1cbe98c1-5956-4656-a03e-97e851a00bb0");

    private static final Instant INSTANT = Instant.parse("2026-07-13T03:00:00Z");

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private AccountTokenService tokenService;

    @Mock
    private AccountMailService mailService;

    @Mock
    private SessionService sessionService;

    private PasswordEncoder passwordEncoder;

    private PasswordAuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(12);
        authService = service(true);
        lenient().when(userRepository.save(any(AppUserEntity.class))).
                thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void rejectsRegistrationWhenEmailAccountsAreDisabled() {
        PasswordAuthService disabledService = service(false);

        assertThatThrownBy(() -> disabledService.register("player@example.com", "long-password", "Player")).
                isInstanceOf(PasswordAuthService.EmailRegistrationDisabledException.class);
    }

    @Test
    void normalizesEmailAndCreatesPendingVerifiedUser() {
        when(userRepository.findByEmailNormalized("player@example.com")).thenReturn(Optional.empty());
        when(tokenService.issue(any(UUID.class), eq(AccountTokenType.EMAIL_VERIFICATION))).
                thenReturn("verification-token");

        AppUserEntity created = authService.register("  PLAYER@Example.COM ", "long-password", " Player ");

        assertThat(created.getEmailNormalized()).isEqualTo("player@example.com");
        assertThat(created.getDisplayName()).isEqualTo("Player");
        assertThat(created.getStatus()).isEqualTo(AppUserStatus.PENDING_VERIFICATION);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getPasswordHash()).startsWith("$2a$12$");
        assertThat(passwordEncoder.matches("long-password", created.getPasswordHash())).isTrue();
        verify(mailService).sendVerification("player@example.com", "verification-token");
    }

    @Test
    void rejectsDuplicateNormalizedEmail() {
        when(userRepository.findByEmailNormalized("player@example.com")).thenReturn(Optional.of(activeUser()));

        assertThatThrownBy(() -> authService.register("PLAYER@example.com", "long-password", "Player")).
                isInstanceOf(PasswordAuthService.DuplicateEmailException.class);
    }

    @Test
    void activatesUserAfterSuccessfulVerification() {
        AppUserEntity user = pendingUser();
        when(tokenService.consume("verification-token", AccountTokenType.EMAIL_VERIFICATION)).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        authService.verifyEmail("verification-token");

        assertThat(user.getStatus()).isEqualTo(AppUserStatus.ACTIVE);
        assertThat(user.getEmailVerifiedAt()).isEqualTo(OffsetDateTime.ofInstant(INSTANT, ZoneOffset.UTC));
    }

    @Test
    void usesSamePublicErrorForUnknownEmailAndWrongPassword() {
        when(userRepository.findByEmailNormalized("missing@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailNormalized("player@example.com")).thenReturn(Optional.of(activeUser()));

        assertThatThrownBy(() -> authService.login("missing@example.com", "wrong-password")).
                isInstanceOf(PasswordAuthService.InvalidCredentialsException.class).
                hasMessage("Invalid email or password.");
        assertThatThrownBy(() -> authService.login("player@example.com", "wrong-password")).
                isInstanceOf(PasswordAuthService.InvalidCredentialsException.class).
                hasMessage("Invalid email or password.");
    }

    @Test
    void logsInActiveUserWithCorrectPassword() {
        AppUserEntity user = activeUser();
        when(userRepository.findByEmailNormalized("player@example.com")).thenReturn(Optional.of(user));

        assertThat(authService.login("PLAYER@example.com", "correct-password")).isSameAs(user);
    }

    @Test
    void resetChangesPasswordAndRevokesEverySession() {
        AppUserEntity user = activeUser();
        when(tokenService.consume("reset-token", AccountTokenType.PASSWORD_RESET)).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        authService.resetPassword("reset-token", "replacement-password");

        assertThat(passwordEncoder.matches("replacement-password", user.getPasswordHash())).isTrue();
        verify(sessionService).revokeAll(USER_ID);
    }

    @Test
    void resetRequestDoesNotRevealUnknownEmail() {
        when(userRepository.findByEmailNormalized("missing@example.com")).thenReturn(Optional.empty());

        authService.requestPasswordReset("missing@example.com");

        verify(tokenService, never()).issue(any(), any());
        verify(mailService, never()).sendPasswordReset(any(), any());
    }

    private PasswordAuthService service(boolean enabled) {
        return new PasswordAuthService(
                userRepository,
                passwordEncoder == null ? new BCryptPasswordEncoder(12) : passwordEncoder,
                tokenService,
                mailService,
                sessionService,
                Clock.fixed(INSTANT, ZoneOffset.UTC),
                enabled
        );
    }

    private AppUserEntity activeUser() {
        AppUserEntity user = pendingUser();
        user.setStatus(AppUserStatus.ACTIVE);
        user.setPasswordHash(passwordEncoder.encode("correct-password"));
        return user;
    }

    private AppUserEntity pendingUser() {
        AppUserEntity user = new AppUserEntity();
        user.setId(USER_ID);
        user.setEmailNormalized("player@example.com");
        user.setDisplayName("Player");
        user.setPasswordHash(passwordEncoder.encode("correct-password"));
        user.setStatus(AppUserStatus.PENDING_VERIFICATION);
        user.setCreatedAt(OffsetDateTime.ofInstant(INSTANT, ZoneOffset.UTC));
        user.setUpdatedAt(OffsetDateTime.ofInstant(INSTANT, ZoneOffset.UTC));
        return user;
    }
}
