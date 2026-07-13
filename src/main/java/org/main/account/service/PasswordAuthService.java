package org.main.account.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import org.main.account.entity.AccountTokenType;
import org.main.account.entity.AppUserEntity;
import org.main.account.entity.AppUserStatus;
import org.main.account.mail.AccountMailService;
import org.main.account.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordAuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password.";

    private final AppUserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AccountTokenService tokenService;

    private final AccountMailService mailService;

    private final SessionService sessionService;

    private final Clock clock;

    private final boolean emailEnabled;

    public PasswordAuthService(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AccountTokenService tokenService,
            AccountMailService mailService,
            SessionService sessionService,
            Clock clock,
            @Value("${app.auth.email-enabled:false}") boolean emailEnabled
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.mailService = mailService;
        this.sessionService = sessionService;
        this.clock = clock;
        this.emailEnabled = emailEnabled;
    }

    @Transactional
    public AppUserEntity register(String email, String password, String displayName) {
        if (!emailEnabled) {
            throw new EmailRegistrationDisabledException();
        }
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.findByEmailNormalized(normalizedEmail).isPresent()) {
            throw new DuplicateEmailException();
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        AppUserEntity user = new AppUserEntity();
        user.setId(UUID.randomUUID());
        user.setEmailNormalized(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(displayName.strip());
        user.setStatus(AppUserStatus.PENDING_VERIFICATION);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);

        String rawToken = tokenService.issue(user.getId(), AccountTokenType.EMAIL_VERIFICATION);
        mailService.sendVerification(normalizedEmail, rawToken);
        return user;
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        UUID userId = tokenService.consume(rawToken, AccountTokenType.EMAIL_VERIFICATION);
        AppUserEntity user = userRepository.findById(userId).
                orElseThrow(AccountTokenService.InvalidAccountTokenException::new);
        OffsetDateTime now = OffsetDateTime.now(clock);
        user.setStatus(AppUserStatus.ACTIVE);
        user.setEmailVerifiedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AppUserEntity login(String email, String password) {
        AppUserEntity user = userRepository.findByEmailNormalized(normalizeEmail(email)).
                filter(candidate -> candidate.getStatus() == AppUserStatus.ACTIVE).
                filter(candidate -> candidate.getPasswordHash() != null).
                orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return user;
    }

    @Transactional
    public void requestPasswordReset(String email) {
        if (!emailEnabled) {
            return;
        }
        userRepository.findByEmailNormalized(normalizeEmail(email)).
                filter(user -> user.getStatus() == AppUserStatus.ACTIVE).
                filter(user -> user.getPasswordHash() != null).
                ifPresent(user -> {
                    String rawToken = tokenService.issue(user.getId(), AccountTokenType.PASSWORD_RESET);
                    mailService.sendPasswordReset(user.getEmailNormalized(), rawToken);
                });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        UUID userId = tokenService.consume(rawToken, AccountTokenType.PASSWORD_RESET);
        AppUserEntity user = userRepository.findById(userId).
                orElseThrow(AccountTokenService.InvalidAccountTokenException::new);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(OffsetDateTime.now(clock));
        userRepository.save(user);
        sessionService.revokeAll(userId);
    }

    private String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    public static class EmailRegistrationDisabledException extends RuntimeException {

        public EmailRegistrationDisabledException() {
            super("Email registration is not available.");
        }
    }

    public static class DuplicateEmailException extends RuntimeException {

        public DuplicateEmailException() {
            super("An account already uses this email.");
        }
    }

    public static class InvalidCredentialsException extends RuntimeException {

        public InvalidCredentialsException() {
            super(INVALID_CREDENTIALS);
        }
    }
}
