package ru.ai.sin.logic.verification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.ai.sin.config.property.TelegramProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBotClient {

    private final TelegramProperties telegramProperties;
    private final org.springframework.web.client.RestClient restClient = org.springframework.web.client.RestClient.create();

    public void sendMessage(long chatId, String text) {
        if (!telegramProperties.isEnabled()) {
            log.debug("Telegram disabled, skip sendMessage to {}", chatId);
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        post("sendMessage", body);
    }

    public void sendContactRequest(long chatId, String text) {
        if (!telegramProperties.isEnabled()) {
            return;
        }
        Map<String, Object> keyboard = Map.of(
                "keyboard", List.of(List.of(Map.of(
                        "text", "Поделиться номером телефона",
                        "request_contact", true
                ))),
                "resize_keyboard", true,
                "one_time_keyboard", true
        );
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        body.put("reply_markup", keyboard);
        post("sendMessage", body);
    }

    public void removeKeyboard(long chatId, String text) {
        if (!telegramProperties.isEnabled()) {
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        body.put("reply_markup", Map.of("remove_keyboard", true));
        post("sendMessage", body);
    }

    private void post(String method, Map<String, Object> body) {
        String token = telegramProperties.getBotToken();
        if (token == null || token.isBlank()) {
            log.warn("Telegram bot token is not configured");
            return;
        }
        try {
            restClient.post()
                    .uri("https://api.telegram.org/bot" + token + "/" + method)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Telegram API {} failed: {}", method, ex.getMessage());
        }
    }
}
