package ru.ai.sin.logic.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import ru.ai.sin.logic.chat.dto.*;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.student.StudentEnt;

import java.util.UUID;

public interface ChatService {

    ChatEnt getOrCreateChat(RecruiterEnt recruiter, StudentEnt student);

    void postSystemMessage(ChatEnt chat, String systemEvent, String body);

    Page<ChatSummaryDTO> listMyChats(Pageable pageable);

    ChatSummaryDTO getChatSummary(UUID chatId);

    Page<ChatMessageDTO> listMessages(UUID chatId, Pageable pageable);

    ChatMessageDTO sendTextMessage(UUID chatId, PostChatMessageReq req);

    ChatMessageDTO sendMessageWithAttachment(UUID chatId, String body, MultipartFile file);

    ChatMessageDTO editMessage(UUID chatId, UUID messageId, PatchChatMessageReq req);

    void adminSoftDeleteMessage(UUID chatId, UUID messageId);

    void markRead(UUID chatId, MarkChatReadReq req);

    ChatContextDTO getChatContext(UUID chatId);

    void adminDeleteChat(UUID chatId);
}
