package ru.ai.sin.logic.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Schema(name = "AnalyticsEventDTO")
public record AnalyticsEventDTO(
        Long id,
        LocalDateTime occurredAt,
        String eventType,
        String path,
        UUID userId,
        UUID sessionId,
        @Schema(description = "Подробности действия. У посещения с сайта пусто")
        Map<String, Object> payload
) {
}
