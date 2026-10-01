package ru.ai.sin.logic.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Текст сообщения (может быть пустым, если есть вложение в отдельном запросе)")
public record PostChatMessageReq(
        @Size(max = 16000)
        String body
) {
}
