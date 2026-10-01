package ru.ai.sin.logic.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AnalyticsPathCountRow", description = "Одна строка агрегата: путь и число событий")
public record AnalyticsPathCountRow(
        @Schema(description = "Значение поля `path` из входящих событий", example = "/resume")
        String path,
        @Schema(description = "Число событий с этим `path` за интервал", example = "42")
        long events) {
}
