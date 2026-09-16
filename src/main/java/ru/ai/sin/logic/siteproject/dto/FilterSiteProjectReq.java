package ru.ai.sin.logic.siteproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(
        name = "FilterSiteProjectReq",
        description = """
                Фильтр списка проектов (`POST /projects/filter`).
                `null` у поля — не ограничивать.
                Для STUDENT/RECRUITER сервер дополнительно оставляет только записи в окне публикации.""")
public record FilterSiteProjectReq(
        @Size(max = 255)
        @Schema(description = "Подстрока по title, summary, body, section и именам навыков")
        String q,

        @Size(max = 255)
        @Schema(description = "Подстрока по разделу / категории")
        String section,

        @Schema(description = "Фильтр по флагу видимости анонимам; null — не фильтровать")
        Boolean visibleToAnonymous
) {
}
