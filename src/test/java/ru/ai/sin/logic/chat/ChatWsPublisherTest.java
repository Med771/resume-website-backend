package ru.ai.sin.logic.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import ru.ai.sin.logic.chat.dto.ChatMessageDTO;
import ru.ai.sin.logic.chat.event.ChatMessagePublishedEvent;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.models.enums.ChatMessageKind;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatWsPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private ChatRepo chatRepo;
    @Mock
    private MessagingGateService messagingGateService;

    private ChatWsPublisher publisher;

    private final UUID chatId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID recruiterId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private final UUID studentId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @BeforeEach
    void setUp() {
        publisher = new ChatWsPublisher(messagingTemplate, chatRepo, messagingGateService);
    }

    @Test
    void publishesSystemMessageToPublicTopic() {
        ChatMessageDTO dto = sampleDto(chatId, ChatMessageKind.SYSTEM);

        publisher.onChatMessage(new ChatMessagePublishedEvent(chatId, dto));

        verify(messagingTemplate).convertAndSend("/topic/chats/" + chatId, dto);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    void publishesUserMessageToStaffTopicWhileMessagingNotAllowed() {
        stubChatAndPendingRequest();
        ChatMessageDTO dto = sampleDto(chatId, ChatMessageKind.USER);

        publisher.onChatMessage(new ChatMessagePublishedEvent(chatId, dto));

        verify(messagingTemplate).convertAndSend("/topic/chats/" + chatId + "/staff", dto);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    void publishesUserMessageToPublicTopicWhenMessagingAllowed() {
        stubChatAndAllowedRequest();
        ChatMessageDTO dto = sampleDto(chatId, ChatMessageKind.USER);

        publisher.onChatMessage(new ChatMessagePublishedEvent(chatId, dto));

        verify(messagingTemplate).convertAndSend("/topic/chats/" + chatId, dto);
        verifyNoMoreInteractions(messagingTemplate);
    }

    private void stubChatAndPendingRequest() {
        RecruiterEnt recruiter = new RecruiterEnt();
        recruiter.setId(recruiterId);
        StudentEnt student = new StudentEnt();
        student.setId(studentId);
        ChatEnt chat = new ChatEnt();
        chat.setRecruiter(recruiter);
        chat.setStudent(student);
        when(chatRepo.findById(chatId)).thenReturn(Optional.of(chat));
        when(messagingGateService.isMessagingAllowed(recruiterId, studentId)).thenReturn(false);
    }

    private void stubChatAndAllowedRequest() {
        RecruiterEnt recruiter = new RecruiterEnt();
        recruiter.setId(recruiterId);
        StudentEnt student = new StudentEnt();
        student.setId(studentId);
        ChatEnt chat = new ChatEnt();
        chat.setRecruiter(recruiter);
        chat.setStudent(student);
        when(chatRepo.findById(chatId)).thenReturn(Optional.of(chat));
        when(messagingGateService.isMessagingAllowed(recruiterId, studentId)).thenReturn(true);
    }

    @Test
    void publishesUnknownNonNullKindToPublicTopicWithWarningPath() {
        ChatMessageDTO dto = new ChatMessageDTO(
                UUID.randomUUID(),
                chatId,
                null,
                null,
                null,
                null,
                "x",
                null,
                java.time.LocalDateTime.now(),
                null,
                null,
                null,
                false
        );

        publisher.onChatMessage(new ChatMessagePublishedEvent(chatId, dto));

        verify(messagingTemplate).convertAndSend("/topic/chats/" + chatId, dto);
    }

    private static ChatMessageDTO sampleDto(UUID chatId, ChatMessageKind kind) {
        return new ChatMessageDTO(
                UUID.randomUUID(),
                chatId,
                null,
                null,
                kind,
                null,
                "text",
                null,
                LocalDateTime.now(),
                null,
                null,
                null,
                false
        );
    }
}
