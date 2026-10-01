package ru.ai.sin.logic.notification.dto;

import ru.ai.sin.models.enums.UserInboxNotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserInboxNotificationDTO(
        UserInboxNotificationType type,
        UUID chatId,
        Long requestId,
        UUID applicationId,
        String preview,
        String systemEvent,
        LocalDateTime occurredAt,
        String counterpartyName
) {
}
