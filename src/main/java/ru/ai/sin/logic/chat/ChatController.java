package ru.ai.sin.logic.chat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.ai.sin.logic.chat.dto.ChatContextDTO;
import ru.ai.sin.logic.chat.dto.ChatMessageDTO;
import ru.ai.sin.logic.chat.dto.ChatSummaryDTO;
import ru.ai.sin.logic.chat.dto.MarkChatReadReq;
import ru.ai.sin.logic.chat.dto.PatchChatMessageReq;
import ru.ai.sin.logic.chat.dto.PostChatMessageReq;
import ru.ai.sin.models.PageResponse;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/chat")
@Tag(name = "Chat", description = "Внутренние чаты рекрутер–студент")
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "Мои чаты")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<PageResponse<ChatSummaryDTO>> listMyChats(@PageableDefault Pageable pageable) {
        Page<ChatSummaryDTO> page = chatService.listMyChats(pageable);
        return ResponseEntity.ok(new PageResponse<>(
                page.getContent(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages()));
    }

    @Operation(summary = "Сводка по чату")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(path = "/{chatId}/summary")
    public ResponseEntity<ChatSummaryDTO> summary(@PathVariable UUID chatId) {
        return ResponseEntity.ok(chatService.getChatSummary(chatId));
    }

    @Operation(summary = "Сообщения чата")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(path = "/{chatId}/messages")
    public ResponseEntity<PageResponse<ChatMessageDTO>> messages(
            @PathVariable UUID chatId,
            @PageableDefault Pageable pageable) {
        Page<ChatMessageDTO> page = chatService.listMessages(chatId, pageable);
        return ResponseEntity.ok(new PageResponse<>(
                page.getContent(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages()));
    }

    @Operation(summary = "Отправить текст")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(path = "/{chatId}/messages")
    public ResponseEntity<ChatMessageDTO> sendText(
            @PathVariable UUID chatId,
            @Valid @RequestBody PostChatMessageReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.sendTextMessage(chatId, req));
    }

    @Operation(summary = "Отправить сообщение с вложением")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(path = "/{chatId}/messages/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChatMessageDTO> sendAttachment(
            @PathVariable UUID chatId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "body", required = false) String body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.sendMessageWithAttachment(chatId, body, file));
    }

    @Operation(summary = "Редактировать своё сообщение")
    @PreAuthorize("isAuthenticated()")
    @PatchMapping(path = "/{chatId}/messages/{messageId}")
    public ResponseEntity<ChatMessageDTO> edit(
            @PathVariable UUID chatId,
            @PathVariable UUID messageId,
            @Valid @RequestBody PatchChatMessageReq req) {
        return ResponseEntity.ok(chatService.editMessage(chatId, messageId, req));
    }

    @Operation(summary = "Контекст чата для админ-панели", description = "Связанные заявки и отклики на вакансии")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(path = "/{chatId}/context")
    public ResponseEntity<ChatContextDTO> context(@PathVariable UUID chatId) {
        return ResponseEntity.ok(chatService.getChatContext(chatId));
    }

    @Operation(summary = "Удалить чат целиком (админ)", description = "Каскадно удаляет заявки, отклики, сообщения и чат")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{chatId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void adminDeleteChat(@PathVariable UUID chatId) {
        chatService.adminDeleteChat(chatId);
    }

    @Operation(summary = "Удалить сообщение (админ, мягкое удаление)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{chatId}/messages/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void adminDelete(@PathVariable UUID chatId, @PathVariable UUID messageId) {
        chatService.adminSoftDeleteMessage(chatId, messageId);
    }

    @Operation(summary = "Отметить прочитанным")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(path = "/{chatId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID chatId, @Valid @RequestBody MarkChatReadReq req) {
        chatService.markRead(chatId, req);
    }
}
