package ru.ai.sin.logic.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PatchChatMessageReq(
        @NotBlank
        @Size(max = 16000)
        String body
) {
}
