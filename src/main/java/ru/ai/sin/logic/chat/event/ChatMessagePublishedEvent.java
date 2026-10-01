package ru.ai.sin.logic.chat.event;

import ru.ai.sin.logic.chat.dto.ChatMessageDTO;

import java.util.UUID;

public record ChatMessagePublishedEvent(UUID chatId, ChatMessageDTO dto) {
}
