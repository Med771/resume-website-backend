package ru.ai.sin.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.ai.sin.config.property.MailProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailStartupValidator implements ApplicationRunner {

    private final MailProperties mailProperties;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (!mailProperties.isEnabled()) {
            return;
        }
        if (mailUsername == null || mailUsername.isBlank()) {
            log.error("APP_MAIL_ENABLED=true, but MAIL_USERNAME is empty — OTP emails will fail");
        }
        if (mailPassword == null || mailPassword.isBlank()) {
            log.error("APP_MAIL_ENABLED=true, but MAIL_PASSWORD is empty — OTP emails will fail");
        }
        String from = mailProperties.getFrom();
        if (from == null || from.isBlank()) {
            log.error("APP_MAIL_ENABLED=true, but MAIL_FROM/MAIL_USERNAME is empty — OTP emails will fail");
        } else if (mailUsername != null && !mailUsername.isBlank()
                && !from.trim().equalsIgnoreCase(mailUsername.trim())) {
            log.warn("MAIL_FROM ({}) differs from MAIL_USERNAME ({}); Yandex may reject messages",
                    from, mailUsername);
        }
    }
}
