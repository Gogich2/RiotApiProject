package org.main.account.service;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.main.account.entity.AppUserStatus;
import org.main.account.entity.UserSessionEntity;
import org.main.account.repository.AppUserRepository;
import org.main.account.repository.UserSessionRepository;
import org.main.account.security.AppPrincipal;
import org.main.account.security.OpaqueTokenCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    private final UserSessionRepository userSessionRepository;

    private final AppUserRepository appUserRepository;

    private final OpaqueTokenCodec tokenCodec;

    private final Clock clock;

    private final Duration sessionDuration;

    public SessionService(
            UserSessionRepository userSessionRepository,
            AppUserRepository appUserRepository,
            OpaqueTokenCodec tokenCodec,
            Clock clock,
            @Value("${app.auth.session-duration:30d}") Duration sessionDuration
    ) {
        this.userSessionRepository = userSessionRepository;
        this.appUserRepository = appUserRepository;
        this.tokenCodec = tokenCodec;
        this.clock = clock;
        this.sessionDuration = sessionDuration;
    }

    @Transactional
    public SessionIssue issue(UUID userId) {
        OffsetDateTime now = now();
        OffsetDateTime expiresAt = now.plus(sessionDuration);
        String rawToken = tokenCodec.generate();

        UserSessionEntity session = new UserSessionEntity();
        session.setId(UUID.randomUUID());
        session.setUserId(userId);
        session.setTokenHash(tokenCodec.hash(rawToken));
        session.setCreatedAt(now);
        session.setLastUsedAt(now);
        session.setExpiresAt(expiresAt);
        userSessionRepository.save(session);
        return new SessionIssue(rawToken, expiresAt);
    }

    @Transactional
    public SessionIssue rotate(String currentRawToken, UUID userId) {
        findActive(currentRawToken).ifPresent(session -> {
            session.setRevokedAt(now());
            userSessionRepository.save(session);
        });
        return issue(userId);
    }

    @Transactional
    public Optional<AppPrincipal> resolve(String rawToken) {
        return findActive(rawToken).flatMap(session -> appUserRepository.findById(session.getUserId()).
                filter(user -> user.getStatus() == AppUserStatus.ACTIVE).
                map(user -> {
                    session.setLastUsedAt(now());
                    userSessionRepository.save(session);
                    return new AppPrincipal(user.getId(), user.getEmailNormalized(), user.getDisplayName());
                }));
    }

    @Transactional
    public void revoke(String rawToken) {
        findActive(rawToken).ifPresent(session -> {
            session.setRevokedAt(now());
            userSessionRepository.save(session);
        });
    }

    @Transactional
    public void revokeAll(UUID userId) {
        userSessionRepository.revokeAllByUserId(userId, now());
    }

    private Optional<UserSessionEntity> findActive(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return userSessionRepository.findActiveByTokenHash(tokenCodec.hash(rawToken), now());
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
