package org.main.account.mail;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class SmtpAccountMailService implements AccountMailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    private final String frontendBaseUrl;

    public SmtpAccountMailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.auth.frontend-base-url:http://localhost:8080}") String frontendBaseUrl
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void sendVerification(String email, String rawToken) {
        send(
                email,
                "Verify your Riot Stats account",
                "Verify your email: " + accountUrl("verify", rawToken)
        );
    }

    @Override
    public void sendPasswordReset(String email, String rawToken) {
        send(
                email,
                "Reset your Riot Stats password",
                "Reset your password: " + accountUrl("reset", rawToken)
        );
    }

    private String accountUrl(String action, String rawToken) {
        return UriComponentsBuilder.fromUriString(frontendBaseUrl).
                path("/account.html").
                queryParam("action", action).
                queryParam("token", rawToken).
                build().
                encode().
                toUriString();
    }

    private void send(String email, String subject, String body) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("Email delivery is not configured");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
