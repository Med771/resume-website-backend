package ru.ai.sin.logic.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(name = "AnalyticsEventsReq", description = "Страница событий журнала за интервал [from, to).")
public record AnalyticsEventsReq(
        @NotNull
        @Schema(description = "Начало интервала, включительно")
        LocalDateTime from,
        @NotNull
        @Schema(description = "Конец интервала, не включая это время")
        LocalDateTime to,
        @Size(max = 64)
        @Schema(description = "Тип события. Пусто — все типы за интервал")
        String eventType,
        @Schema(description = "Номер страницы с нуля. Пусто — 0")
        Integer page,
        @Schema(description = "Размер страницы. Пусто — 20, больше 100 обрезается до 100")
        Integer size
) {
}
