package ru.ai.sin.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.telegram")
public class TelegramProperties {

    private boolean enabled = false;
    private String botToken = "";
    private String botUsername = "";
    private String webhookSecret = "";
    private int verificationTtlMinutes = 15;
    /** Только для локальных тестов: подтверждение кодом без Telegram */
    private boolean allowDevConfirm = false;
    private String devConfirmCode = "7890";
}
