package org.main.account.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.main.account.dto.AuthErrorResponse;
import org.main.account.dto.AuthUserResponse;
import org.main.account.dto.CurrentUserResponse;
import org.main.account.dto.LoginRequest;
import org.main.account.dto.PasswordResetConfirmRequest;
import org.main.account.dto.PasswordResetRequest;
import org.main.account.dto.RegisterRequest;
import org.main.account.dto.TokenRequest;
import org.main.account.entity.AppUserEntity;
import org.main.account.security.AppPrincipal;
import org.main.account.security.AuthRateLimiter;
import org.main.account.service.AccountTokenService;
import org.main.account.service.PasswordAuthService;
import org.main.account.service.SessionIssue;
import org.main.account.service.SessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final PasswordAuthService authService;

    private final SessionService sessionService;

    private final AuthRateLimiter rateLimiter;

    private final String cookieName;

    private final boolean secureCookie;

    private final Duration sessionDuration;

    public AuthController(
            PasswordAuthService authService,
            SessionService sessionService,
            AuthRateLimiter rateLimiter,
            @Value("${app.auth.session-cookie-name:RIOT_STATS_SESSION}") String cookieName,
            @Value("${app.auth.secure-cookie:false}") boolean secureCookie,
            @Value("${app.auth.session-duration:30d}") Duration sessionDuration
    ) {
        this.authService = authService;
        this.sessionService = sessionService;
        this.rateLimiter = rateLimiter;
        this.cookieName = cookieName;
        this.secureCookie = secureCookie;
        this.sessionDuration = sessionDuration;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest
    ) {
        rateLimiter.check(servletRequest.getRemoteAddr(), "register");
        authService.register(request.email(), request.password(), request.displayName());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody TokenRequest request) {
        authService.verifyEmail(request.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<CurrentUserResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        rateLimiter.check(servletRequest.getRemoteAddr(), "login");
        AppUserEntity user = authService.login(request.email(), request.password());
        Cookie currentCookie = WebUtils.getCookie(servletRequest, cookieName);
        SessionIssue session = currentCookie == null
                ? sessionService.issue(user.getId())
                : sessionService.rotate(currentCookie.getValue(), user.getId());
        return ResponseEntity.ok().
                header(HttpHeaders.SET_COOKIE, sessionCookie(session.rawToken(), sessionDuration).toString()).
                body(CurrentUserResponse.authenticated(AuthUserResponse.from(user)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, cookieName);
        if (cookie != null) {
            sessionService.revoke(cookie.getValue());
        }
        return ResponseEntity.noContent().
                header(HttpHeaders.SET_COOKIE, sessionCookie("", Duration.ZERO).toString()).
                build();
    }

    @GetMapping("/me")
    public CurrentUserResponse currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppPrincipal principal)) {
            return CurrentUserResponse.anonymous();
        }
        return CurrentUserResponse.authenticated(AuthUserResponse.from(principal));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request
    ) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(AuthRateLimiter.AuthRateLimitException.class)
    public ResponseEntity<AuthErrorResponse> rateLimited(
            AuthRateLimiter.AuthRateLimitException exception
    ) {
        long retrySeconds = Math.max(1, exception.getRetryAfter().toSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).
                header(HttpHeaders.RETRY_AFTER, Long.toString(retrySeconds)).
                body(new AuthErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(PasswordAuthService.InvalidCredentialsException.class)
    public ResponseEntity<AuthErrorResponse> invalidCredentials(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).
                body(new AuthErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler({
            AccountTokenService.InvalidAccountTokenException.class,
            PasswordAuthService.DuplicateEmailException.class
    })
    public ResponseEntity<AuthErrorResponse> invalidAccountRequest(RuntimeException exception) {
        HttpStatus status = exception instanceof PasswordAuthService.DuplicateEmailException
                ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new AuthErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(PasswordAuthService.EmailRegistrationDisabledException.class)
    public ResponseEntity<AuthErrorResponse> emailRegistrationDisabled(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).
                body(new AuthErrorResponse(exception.getMessage()));
    }

    private ResponseCookie sessionCookie(String value, Duration maxAge) {
        return ResponseCookie.from(cookieName, value).
                httpOnly(true).
                secure(secureCookie).
                sameSite("Lax").
                path("/").
                maxAge(maxAge).
                build();
    }
}
