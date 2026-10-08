package ru.ai.sin.logic.verification;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import ru.ai.sin.config.property.MailProperties;
import ru.ai.sin.exception.models.BadRequestException;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationOtpMailer {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public void sendOtp(String toEmail, String code, int ttlMinutes) {
        if (!trySendOtp(toEmail, code, ttlMinutes)) {
            throw new BadRequestException("Не удалось отправить код на почту. Попробуйте позже.");
        }
    }

    /**
     * @return true if the message was sent successfully
     */
    public boolean trySendOtp(String toEmail, String code, int ttlMinutes) {
        return trySend(
                toEmail,
                "Код подтверждения — Singularity Resume",
                buildBody(code, ttlMinutes),
                "Verification OTP");
    }

    public boolean trySendPasswordReset(String toEmail, String code, int ttlMinutes, String username) {
        return trySend(
                toEmail,
                "Код для смены пароля — Singularity Resume",
                buildPasswordResetBody(code, ttlMinutes, username),
                "Password reset");
    }

    private boolean trySend(String toEmail, String subject, String body, String logLabel) {
        if (!mailProperties.isEnabled()) {
            log.warn("{} email skipped: app.mail.enabled=false", logLabel);
            return false;
        }
        String from = mailProperties.getFrom();
        if (from == null || from.isBlank()) {
            log.warn("{} email skipped: app.mail.from is not configured", logLabel);
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from.trim());
            helper.setTo(toEmail.trim());
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            log.info("{} email sent to {}", logLabel, maskEmail(toEmail));
            return true;
        } catch (Exception ex) {
            log.warn("Failed to send {} email to {}: {}", logLabel, maskEmail(toEmail), describeMailError(ex));
            return false;
        }
    }

    private static String describeMailError(Exception ex) {
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + ": " + root.getMessage();
    }

    private static String buildBody(String code, int ttlMinutes) {
        return """
                Здравствуйте!

                Ваш код подтверждения: %s

                Код действует %d мин. Никому не сообщайте этот код.

                Если вы не запрашивали код, просто проигнорируйте это письмо.
                """.formatted(code, ttlMinutes);
    }

    private static String buildPasswordResetBody(String code, int ttlMinutes, String username) {
        String login = username == null ? "" : username;
        return """
                Здравствуйте!

                Код для смены пароля: %s

                Ваш логин: %s

                Код действует %d мин. Никому не сообщайте этот код.

                Если вы не запрашивали смену пароля, просто проигнорируйте это письмо.
                """.formatted(code, login, ttlMinutes);
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String maskedLocal = local.length() <= 2 ? "**" : local.charAt(0) + "***";
        return maskedLocal + email.substring(at);
    }
}
