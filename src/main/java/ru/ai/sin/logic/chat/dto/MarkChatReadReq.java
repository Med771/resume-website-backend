package ru.ai.sin.logic.chat.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MarkChatReadReq(
        @NotNull
        UUID messageId
) {
}
