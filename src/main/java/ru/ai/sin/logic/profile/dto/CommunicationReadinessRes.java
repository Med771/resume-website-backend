package ru.ai.sin.logic.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "CommunicationReadinessRes", description = "Готовность профиля к переписке")
public record CommunicationReadinessRes(
        @Schema(description = "Профиль достаточно заполнен для чатов")
        boolean ready,
        @Schema(description = "Незаполненные поля (id для UI)")
        List<String> missingFields
) {
}
