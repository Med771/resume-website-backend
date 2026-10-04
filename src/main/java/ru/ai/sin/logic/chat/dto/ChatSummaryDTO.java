package ru.ai.sin.logic.chat.dto;

import ru.ai.sin.models.enums.ResultEnum;
import ru.ai.sin.models.enums.TuPhase;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatSummaryDTO(
        UUID id,
        UUID recruiterId,
        UUID studentId,
        String lastMessagePreview,
        LocalDateTime lastActivityAt,
        long unreadCount,
        String recruiterDisplayName,
        String studentDisplayName,
        Long activeRequestId,
        ResultEnum activeRequestResult,
        TuPhase tuPhase,
        long messageCount
) {
}
