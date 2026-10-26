package ru.ai.sin.logic.main.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MainStatusRes", description = "Публичный статус сервиса")
public record MainStatusRes(
        @Schema(description = "Состояние сервиса", example = "UP")
        String status,
        @Schema(description = "Версия приложения", example = "1.0.0")
        String version
) {
}
