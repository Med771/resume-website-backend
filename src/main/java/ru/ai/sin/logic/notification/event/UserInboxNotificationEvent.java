package ru.ai.sin.logic.notification.event;

import ru.ai.sin.logic.notification.dto.UserInboxNotificationDTO;

import java.util.UUID;

public record UserInboxNotificationEvent(UUID recipientUserId, UserInboxNotificationDTO notification) {
}
