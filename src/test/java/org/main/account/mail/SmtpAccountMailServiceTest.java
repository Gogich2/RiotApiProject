package org.main.account.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class SmtpAccountMailServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender mailSender;

    @Test
    void sendsRawTokenOnlyInsideConfiguredVerificationLink() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        SmtpAccountMailService mailService = new SmtpAccountMailService(
                mailSenderProvider,
                "https://stats.example"
        );

        mailService.sendVerification("player@example.com", "raw token");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("player@example.com");
        assertThat(captor.getValue().getText()).contains(
                "https://stats.example/account.html?action=verify&token=raw%20token"
        );
    }
}
