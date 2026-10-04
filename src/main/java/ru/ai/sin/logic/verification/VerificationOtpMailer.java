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
        if (!mailProperties.isEnabled()) {
            log.warn("Verification OTP email skipped: app.mail.enabled=false");
            return false;
        }
        String from = mailProperties.getFrom();
        if (from == null || from.isBlank()) {
            log.warn("Verification OTP email skipped: app.mail.from is not configured");
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from.trim());
            helper.setTo(toEmail.trim());
            helper.setSubject("Код подтверждения — Singularity Resume");
            helper.setText(buildBody(code, ttlMinutes), false);
            mailSender.send(message);
            log.info("Verification OTP email sent to {}", maskEmail(toEmail));
            return true;
        } catch (Exception ex) {
            log.warn("Failed to send verification OTP to {}: {}", maskEmail(toEmail), describeMailError(ex));
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

    static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String maskedLocal = local.length() <= 2 ? "**" : local.charAt(0) + "***";
        return maskedLocal + email.substring(at);
    }
}
