package ru.ai.sin.logic.verification;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ai.sin.config.property.TelegramProperties;

import java.util.Map;

@Hidden
@RestController
@RequestMapping("/telegram")
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final PhoneVerificationService phoneVerificationService;
    private final TelegramProperties telegramProperties;

    @GetMapping("/webhook")
    public ResponseEntity<Void> webhookHealthCheck() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secret,
            @RequestBody Map<String, Object> update
    ) {
        String expected = telegramProperties.getWebhookSecret();
        if (expected != null && !expected.isBlank() && !expected.equals(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        phoneVerificationService.handleWebhookUpdate(update);
        return ResponseEntity.ok().build();
    }
}
