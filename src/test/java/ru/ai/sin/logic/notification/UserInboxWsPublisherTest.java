package ru.ai.sin.logic.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import ru.ai.sin.logic.notification.dto.UserInboxNotificationDTO;
import ru.ai.sin.logic.notification.event.UserInboxNotificationEvent;
import ru.ai.sin.models.enums.UserInboxNotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserInboxWsPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private UserInboxWsPublisher publisher;

    @Test
    void publishesToUserInboxTopic() {
        UUID userId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        var dto = new UserInboxNotificationDTO(
                UserInboxNotificationType.NEW_REQUEST,
                chatId,
                42L,
                null,
                "preview",
                "REQUEST_SENT",
                LocalDateTime.now(),
                "Recruiter"
        );

        publisher.onInboxNotification(new UserInboxNotificationEvent(userId, dto));

        verify(messagingTemplate).convertAndSend(
                eq("/topic/users/" + userId + "/inbox"),
                eq(dto));
    }
}
