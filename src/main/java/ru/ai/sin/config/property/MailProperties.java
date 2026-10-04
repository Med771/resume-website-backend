package ru.ai.sin.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    private boolean enabled = false;
    private String from = "";
    /** TTL кода подтверждения почты после саморегистрации студента. */
    private int otpTtlMinutes = 15;
    /** Для тестов/стенда: не требует реальной отправки письма. */
    private boolean allowDevConfirm = false;
}
