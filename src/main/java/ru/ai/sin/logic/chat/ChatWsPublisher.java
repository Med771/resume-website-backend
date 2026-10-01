package ru.ai.sin.logic.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.ai.sin.logic.chat.event.ChatMessagePublishedEvent;
import ru.ai.sin.models.enums.ChatMessageKind;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWsPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRepo chatRepo;
    private final MessagingGateService messagingGateService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onChatMessage(ChatMessagePublishedEvent event) {
        UUID chatId = event.chatId();
        var dto = event.dto();
        String publicTopic = "/topic/chats/" + chatId;
        if (dto.messageKind() == ChatMessageKind.SYSTEM) {
            messagingTemplate.convertAndSend(publicTopic, dto);
            return;
        }
        if (dto.messageKind() == ChatMessageKind.USER) {
            if (isMessagingAllowed(chatId)) {
                messagingTemplate.convertAndSend(publicTopic, dto);
            } else {
                messagingTemplate.convertAndSend(publicTopic + "/staff", dto);
            }
            return;
        }
        if (dto.messageKind() != null) {
            log.warn("WS publish: неизвестный messageKind={}, chatId={}", dto.messageKind(), chatId);
        }
        messagingTemplate.convertAndSend(publicTopic, dto);
    }

    private boolean isMessagingAllowed(UUID chatId) {
        return chatRepo.findById(chatId)
                .map(chat -> messagingGateService.isMessagingAllowed(
                        chat.getRecruiter().getId(),
                        chat.getStudent().getId()))
                .orElse(false);
    }
}
