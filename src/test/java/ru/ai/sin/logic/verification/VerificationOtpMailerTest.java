package ru.ai.sin.logic.verification;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import ru.ai.sin.config.property.MailProperties;
import ru.ai.sin.exception.models.BadRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationOtpMailerTest {

    @Mock
    private JavaMailSender mailSender;

    private MailProperties mailProperties;
    private VerificationOtpMailer mailer;

    @BeforeEach
    void setUp() {
        mailProperties = new MailProperties();
        mailProperties.setEnabled(true);
        mailProperties.setFrom("med77.2@ya.ru");
        mailer = new VerificationOtpMailer(mailSender, mailProperties);
    }

    @Test
    void sendOtp_buildsMessage() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        mailer.sendOtp("recipient@example.com", "1234", 15);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue()).isSameAs(mimeMessage);
    }

    @Test
    void sendOtp_requiresEnabledMail() {
        mailProperties.setEnabled(false);
        assertThat(mailer.trySendOtp("a@b.c", "1234", 15)).isFalse();
        assertThatThrownBy(() -> mailer.sendOtp("a@b.c", "1234", 15))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void maskEmail_hidesLocalPart() {
        assertThat(VerificationOtpMailer.maskEmail("med77.2@ya.ru")).isEqualTo("m***@ya.ru");
    }
}
