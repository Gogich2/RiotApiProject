package org.main.account.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.main.account.entity.AccountActionTokenEntity;
import org.main.account.entity.AccountTokenType;
import org.main.account.entity.AppUserEntity;
import org.main.account.entity.AppUserStatus;
import org.main.account.entity.OAuthIdentityEntity;
import org.main.account.entity.SavedProfileEntity;
import org.main.account.entity.UserSessionEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DirtiesContext
@Transactional
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "app.scheduler.background-maintenance.enabled=false",
        "app.scheduler.match-analysis.enabled=false"
})
class AccountRepositoryIT {

    private static final OffsetDateTime NOW = OffsetDateTime.of(
            2026, 7, 13, 2, 30, 0, 0, ZoneOffset.UTC);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private OAuthIdentityRepository oAuthIdentityRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private AccountActionTokenRepository accountActionTokenRepository;

    @Autowired
    private SavedProfileRepository savedProfileRepository;

    @Test
    void findsUserByNormalizedEmailAndRejectsDuplicates() {
        AppUserEntity first = saveUser("player@example.com");

        assertThat(appUserRepository.findByEmailNormalized("player@example.com")).
                contains(first);

        AppUserEntity duplicate = newUser("player@example.com");
        assertThatThrownBy(() -> appUserRepository.saveAndFlush(duplicate)).
                isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsDiscordIdentityByImmutableProviderSubject() {
        AppUserEntity user = saveUser("discord@example.com");
        OAuthIdentityEntity identity = new OAuthIdentityEntity();
        identity.setId(UUID.randomUUID());
        identity.setUserId(user.getId());
        identity.setProvider("discord");
        identity.setProviderSubjectId("1234567890");
        identity.setCreatedAt(NOW);
        identity.setLastLoginAt(NOW);
        oAuthIdentityRepository.saveAndFlush(identity);

        assertThat(oAuthIdentityRepository.findByProviderAndProviderSubjectId(
                "discord", "1234567890")).
                map(OAuthIdentityEntity::getId).
                contains(identity.getId());
    }

    @Test
    void resolvesOnlyActiveUnexpiredSessions() {
        AppUserEntity user = saveUser("session@example.com");
        UserSessionEntity active = newSession(user.getId(), "a".repeat(64), NOW.plusHours(1));
        UserSessionEntity expired = newSession(user.getId(), "b".repeat(64), NOW.minusSeconds(1));
        userSessionRepository.saveAllAndFlush(java.util.List.of(active, expired));

        assertThat(userSessionRepository.findActiveByTokenHash("a".repeat(64), NOW)).
                map(UserSessionEntity::getId).
                contains(active.getId());
        assertThat(userSessionRepository.findActiveByTokenHash("b".repeat(64), NOW)).
                isEmpty();
    }

    @Test
    void resolvesOnlyUsableSingleUseActionTokens() {
        AppUserEntity user = saveUser("token@example.com");
        AccountActionTokenEntity usable = newToken(user.getId(), "c".repeat(64), NOW.plusMinutes(5));
        AccountActionTokenEntity consumed = newToken(user.getId(), "d".repeat(64), NOW.plusMinutes(5));
        consumed.setConsumedAt(NOW.minusMinutes(1));
        accountActionTokenRepository.saveAllAndFlush(java.util.List.of(usable, consumed));

        assertThat(accountActionTokenRepository.findUsableByTokenHashAndTokenType(
                "c".repeat(64), AccountTokenType.EMAIL_VERIFICATION, NOW)).
                map(AccountActionTokenEntity::getId).
                contains(usable.getId());
        assertThat(accountActionTokenRepository.findUsableByTokenHashAndTokenType(
                "d".repeat(64), AccountTokenType.EMAIL_VERIFICATION, NOW)).isEmpty();
    }

    @Test
    void preventsDuplicateSavedProfileForSameUser() {
        AppUserEntity user = saveUser("saved@example.com");
        SavedProfileEntity first = newSavedProfile(user.getId(), "puuid-1");
        savedProfileRepository.saveAndFlush(first);

        assertThat(savedProfileRepository.findByUserIdAndPuuid(user.getId(), "puuid-1")).
                map(SavedProfileEntity::getId).
                contains(first.getId());
        assertThatThrownBy(() -> savedProfileRepository.saveAndFlush(
                newSavedProfile(user.getId(), "puuid-1"))).
                isInstanceOf(DataIntegrityViolationException.class);
    }

    private AppUserEntity saveUser(String email) {
        return appUserRepository.saveAndFlush(newUser(email));
    }

    private AppUserEntity newUser(String email) {
        AppUserEntity user = new AppUserEntity();
        user.setId(UUID.randomUUID());
        user.setEmailNormalized(email);
        user.setDisplayName("Player");
        user.setStatus(AppUserStatus.ACTIVE);
        user.setCreatedAt(NOW);
        user.setUpdatedAt(NOW);
        return user;
    }

    private UserSessionEntity newSession(UUID userId, String hash, OffsetDateTime expiry) {
        UserSessionEntity session = new UserSessionEntity();
        session.setId(UUID.randomUUID());
        session.setUserId(userId);
        session.setTokenHash(hash);
        session.setExpiresAt(expiry);
        session.setLastUsedAt(NOW);
        session.setCreatedAt(NOW);
        return session;
    }

    private AccountActionTokenEntity newToken(UUID userId, String hash, OffsetDateTime expiry) {
        AccountActionTokenEntity token = new AccountActionTokenEntity();
        token.setId(UUID.randomUUID());
        token.setUserId(userId);
        token.setTokenType(AccountTokenType.EMAIL_VERIFICATION);
        token.setTokenHash(hash);
        token.setExpiresAt(expiry);
        token.setCreatedAt(NOW);
        return token;
    }

    private SavedProfileEntity newSavedProfile(UUID userId, String puuid) {
        SavedProfileEntity profile = new SavedProfileEntity();
        profile.setId(UUID.randomUUID());
        profile.setUserId(userId);
        profile.setPuuid(puuid);
        profile.setDefault(false);
        profile.setSavedAt(NOW);
        profile.setLastViewedAt(NOW);
        return profile;
    }
}
