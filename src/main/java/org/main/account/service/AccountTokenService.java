package org.main.account.service;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.main.account.entity.AccountActionTokenEntity;
import org.main.account.entity.AccountTokenType;
import org.main.account.repository.AccountActionTokenRepository;
import org.main.account.security.OpaqueTokenCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountTokenService {

    private final AccountActionTokenRepository tokenRepository;

    private final OpaqueTokenCodec tokenCodec;

    private final Clock clock;

    private final Duration verificationDuration;

    private final Duration resetDuration;

    public AccountTokenService(
            AccountActionTokenRepository tokenRepository,
            OpaqueTokenCodec tokenCodec,
            Clock clock,
            @Value("${app.auth.verification-token-duration:24h}") Duration verificationDuration,
            @Value("${app.auth.reset-token-duration:30m}") Duration resetDuration
    ) {
        this.tokenRepository = tokenRepository;
        this.tokenCodec = tokenCodec;
        this.clock = clock;
        this.verificationDuration = verificationDuration;
        this.resetDuration = resetDuration;
    }

    @Transactional
    public String issue(UUID userId, AccountTokenType tokenType) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        String rawToken = tokenCodec.generate();

        AccountActionTokenEntity token = new AccountActionTokenEntity();
        token.setId(UUID.randomUUID());
        token.setUserId(userId);
        token.setTokenType(tokenType);
        token.setTokenHash(tokenCodec.hash(rawToken));
        token.setCreatedAt(now);
        token.setExpiresAt(now.plus(durationFor(tokenType)));
        tokenRepository.save(token);
        return rawToken;
    }

    @Transactional
    public UUID consume(String rawToken, AccountTokenType tokenType) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidAccountTokenException();
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        AccountActionTokenEntity token = tokenRepository.findUsableByTokenHashAndTokenType(
                tokenCodec.hash(rawToken),
                tokenType,
                now
        ).orElseThrow(InvalidAccountTokenException::new);
        token.setConsumedAt(now);
        tokenRepository.save(token);
        return token.getUserId();
    }

    private Duration durationFor(AccountTokenType tokenType) {
        return tokenType == AccountTokenType.EMAIL_VERIFICATION ? verificationDuration : resetDuration;
    }

    public static class InvalidAccountTokenException extends RuntimeException {

        public InvalidAccountTokenException() {
            super("This account link is invalid or has expired.");
        }
    }
}
