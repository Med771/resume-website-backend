package ru.ai.sin.logic.chat.dto;

import ru.ai.sin.models.enums.ChatMessageKind;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatMessageDTO(
        UUID id,
        UUID chatId,
        UUID authorUserId,
        String authorUsername,
        ChatMessageKind messageKind,
        String systemEvent,
        String body,
        String attachmentStorageName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime editedAt,
        LocalDateTime deletedAt,
        boolean deletedByAdmin
) {
}
