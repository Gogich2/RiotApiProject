package org.main.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.main.account.entity.AccountActionTokenEntity;
import org.main.account.entity.AccountTokenType;
import org.main.account.repository.AccountActionTokenRepository;
import org.main.account.security.OpaqueTokenCodec;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountTokenServiceTest {

    private static final UUID USER_ID = UUID.fromString("1cbe98c1-5956-4656-a03e-97e851a00bb0");

    private static final Instant INSTANT = Instant.parse("2026-07-13T03:00:00Z");

    @Mock
    private AccountActionTokenRepository tokenRepository;

    private OpaqueTokenCodec tokenCodec;

    private AccountTokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenCodec = new OpaqueTokenCodec();
        tokenService = new AccountTokenService(
                tokenRepository,
                tokenCodec,
                Clock.fixed(INSTANT, ZoneOffset.UTC),
                Duration.ofHours(24),
                Duration.ofMinutes(30)
        );
    }

    @Test
    void issuesHashedVerificationTokenWithTwentyFourHourExpiry() {
        String rawToken = tokenService.issue(USER_ID, AccountTokenType.EMAIL_VERIFICATION);
        ArgumentCaptor<AccountActionTokenEntity> captor = ArgumentCaptor.forClass(AccountActionTokenEntity.class);
        verify(tokenRepository).save(captor.capture());

        assertThat(captor.getValue().getTokenHash()).isEqualTo(tokenCodec.hash(rawToken));
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo(rawToken);
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(
                OffsetDateTime.ofInstant(INSTANT.plus(Duration.ofHours(24)), ZoneOffset.UTC)
        );
    }

    @Test
    void consumesUsableTokenOnlyOnce() {
        AccountActionTokenEntity token = new AccountActionTokenEntity();
        token.setUserId(USER_ID);
        when(tokenRepository.findUsableByTokenHashAndTokenType(any(), any(), any())).
                thenReturn(Optional.of(token));

        assertThat(tokenService.consume("raw-token", AccountTokenType.PASSWORD_RESET)).isEqualTo(USER_ID);
        assertThat(token.getConsumedAt()).isEqualTo(OffsetDateTime.ofInstant(INSTANT, ZoneOffset.UTC));
        verify(tokenRepository).save(token);
    }

    @Test
    void rejectsExpiredConsumedOrUnknownToken() {
        when(tokenRepository.findUsableByTokenHashAndTokenType(any(), any(), any())).
                thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.consume("expired-token", AccountTokenType.PASSWORD_RESET)).
                isInstanceOf(AccountTokenService.InvalidAccountTokenException.class);
    }
}
