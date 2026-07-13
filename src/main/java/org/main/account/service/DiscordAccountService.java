package org.main.account.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.main.account.entity.AppUserEntity;
import org.main.account.entity.AppUserStatus;
import org.main.account.entity.OAuthIdentityEntity;
import org.main.account.repository.AppUserRepository;
import org.main.account.repository.OAuthIdentityRepository;
import org.main.account.security.AppPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiscordAccountService {

    private static final String PROVIDER = "discord";

    private final AppUserRepository userRepository;

    private final OAuthIdentityRepository identityRepository;

    private final Clock clock;

    private final boolean discordEnabled;

    public DiscordAccountService(
            AppUserRepository userRepository,
            OAuthIdentityRepository identityRepository,
            Clock clock,
            @Value("${app.auth.discord-enabled:false}") boolean discordEnabled
    ) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.clock = clock;
        this.discordEnabled = discordEnabled;
    }

    @Transactional
    public AppPrincipal authenticate(Map<String, Object> attributes) {
        if (!discordEnabled) {
            throw new DiscordAccountException();
        }
        String subjectId = requiredText(attributes.get("id"));
        String email = requiredText(attributes.get("email")).toLowerCase(Locale.ROOT);
        if (!Boolean.TRUE.equals(attributes.get("verified"))) {
            throw new DiscordAccountException();
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        return identityRepository.findByProviderAndProviderSubjectId(PROVIDER, subjectId).
                map(identity -> existingIdentity(identity, now)).
                orElseGet(() -> createIdentity(attributes, subjectId, email, now));
    }

    private AppPrincipal existingIdentity(OAuthIdentityEntity identity, OffsetDateTime now) {
        AppUserEntity user = userRepository.findById(identity.getUserId()).
                filter(candidate -> candidate.getStatus() == AppUserStatus.ACTIVE).
                orElseThrow(DiscordAccountException::new);
        identity.setLastLoginAt(now);
        identityRepository.save(identity);
        return principal(user);
    }

    private AppPrincipal createIdentity(
            Map<String, Object> attributes,
            String subjectId,
            String email,
            OffsetDateTime now
    ) {
        AppUserEntity user = userRepository.findByEmailNormalized(email).
                map(existing -> {
                    if (existing.getStatus() != AppUserStatus.ACTIVE) {
                        throw new DiscordAccountException();
                    }
                    return existing;
                }).
                orElseGet(() -> createUser(attributes, email, now));

        OAuthIdentityEntity identity = new OAuthIdentityEntity();
        identity.setId(UUID.randomUUID());
        identity.setUserId(user.getId());
        identity.setProvider(PROVIDER);
        identity.setProviderSubjectId(subjectId);
        identity.setCreatedAt(now);
        identity.setLastLoginAt(now);
        identityRepository.save(identity);
        return principal(user);
    }

    private AppUserEntity createUser(Map<String, Object> attributes, String email, OffsetDateTime now) {
        AppUserEntity user = new AppUserEntity();
        user.setId(UUID.randomUUID());
        user.setEmailNormalized(email);
        user.setDisplayName(displayName(attributes));
        user.setStatus(AppUserStatus.ACTIVE);
        user.setEmailVerifiedAt(now);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    private String displayName(Map<String, Object> attributes) {
        String name = optionalText(attributes.get("global_name"));
        if (name == null) {
            name = optionalText(attributes.get("username"));
        }
        if (name == null) {
            name = "Discord user";
        }
        return name.length() <= 60 ? name : name.substring(0, 60);
    }

    private String requiredText(Object value) {
        String text = optionalText(value);
        if (text == null) {
            throw new DiscordAccountException();
        }
        return text;
    }

    private String optionalText(Object value) {
        if (!(value instanceof String text) || text.isBlank()) {
            return null;
        }
        return text.strip();
    }

    private AppPrincipal principal(AppUserEntity user) {
        return new AppPrincipal(user.getId(), user.getEmailNormalized(), user.getDisplayName());
    }

    public static class DiscordAccountException extends RuntimeException {

        public DiscordAccountException() {
            super("Discord sign-in could not be completed.");
        }
    }
}
