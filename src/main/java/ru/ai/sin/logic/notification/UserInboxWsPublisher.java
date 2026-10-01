package ru.ai.sin.logic.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.ai.sin.logic.notification.event.UserInboxNotificationEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserInboxWsPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInboxNotification(UserInboxNotificationEvent event) {
        String topic = "/topic/users/" + event.recipientUserId() + "/inbox";
        messagingTemplate.convertAndSend(topic, event.notification());
        log.debug("Inbox WS -> {} type={}", topic, event.notification().type());
    }
}
