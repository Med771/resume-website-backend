package ru.ai.sin.logic.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(
        name = "AnalyticsSummaryReq",
        description = "Запрос агрегированной статистики за интервал времени для `POST /admin/analytics/summary` (только ADMIN).")
public record AnalyticsSummaryReq(
        @NotNull
        @Schema(description = "Начало интервала, **включительно** (`>= from`)")
        LocalDateTime from,
        @NotNull
        @Schema(description = "Конец интервала, **исключительно** (`< to`); типичный паттерн — полуоткрытый интервал как в SQL `WHERE t >= :from AND t < :to`")
        LocalDateTime to
) {
}
