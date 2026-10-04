package ru.ai.sin.logic.siteproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.List;

@Schema(
        name = "UpdateSiteProjectReq",
        description = "Полная замена полей проекта (`PUT /projects/{id}`), только ADMIN. Не PATCH.")
public record UpdateSiteProjectReq(
        @NotBlank @Size(max = 255)
        @Schema(description = "Заголовок")
        String title,
        @Size(max = 255)
        @Schema(description = "Раздел / категория")
        String section,
        @Schema(description = "Краткое описание")
        String summary,
        @Schema(description = "Полный текст")
        String body,
        @Valid
        @Schema(description = "Галерея изображений (полная замена списка)")
        List<SiteProjectImageReq> images,
        @Schema(description = "ID навыков из справочника; полная замена списка")
        List<@Positive Long> skillIds,
        @Schema(description = "Видимость анонимам на `GET /public/vitrina/home`")
        boolean visibleToAnonymous,
        @Schema(description = "Начало окна публикации")
        LocalDateTime publishedFrom,
        @Schema(description = "Конец окна публикации")
        LocalDateTime publishedTo
) {
}
