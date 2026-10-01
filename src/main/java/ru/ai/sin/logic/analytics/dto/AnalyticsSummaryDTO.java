package ru.ai.sin.logic.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "AnalyticsSummaryDTO", description = "Ответ со сводкой: количество событий по каждому `path` за запрошенный интервал")
public record AnalyticsSummaryDTO(
        @Schema(description = "Список строк агрегации")
        List<AnalyticsPathCountRow> byPath) {
}
