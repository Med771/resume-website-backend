package ru.ai.sin.logic.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(
        name = "EntityPopulationSummaryReq",
        description = """
                Запрос сводки по числу записей в таблицах `users`, `students`, `recruiters`.

                Поля **`from`** / **`to`**: оба **`null`** — только текущие итоги (без окна по дате создания).
                Оба заданы — дополнительно считаются **новые** студенты и рекрутеры за полуинтервал **[from, to)** по `created_at` в соответствующих таблицах.
                У пользователей в схеме **нет** `created_at` — разбивка «новых пользователей за период» в этом ответе **не** считается.""")
public record EntityPopulationSummaryReq(
        @Schema(description = "Начало окна (включитель); должен быть задан вместе с `to`", example = "2026-01-01T00:00:00")
        LocalDateTime from,

        @Schema(description = "Конец окна (исключительно); должен быть строго после `from`", example = "2026-02-01T00:00:00")
        LocalDateTime to
) {
}
