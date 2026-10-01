package ru.ai.sin.logic.siteproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.List;

@Schema(
        name = "CreateSiteProjectReq",
        description = "Тело создания проекта для ленты (`POST /projects`), только ADMIN.")
public record CreateSiteProjectReq(
        @NotBlank @Size(max = 255)
        @Schema(description = "Заголовок карточки проекта")
        String title,
        @Size(max = 255)
        @Schema(description = "Раздел / категория (например «Веб-разработка»)")
        String section,
        @Schema(description = "Краткое описание (подзаголовок), может быть null")
        String summary,
        @Schema(description = "Полный текст / HTML по соглашению фронта, может быть null")
        String body,
        @Valid
        @Schema(description = "Галерея изображений (файл из хранилища и/или URL)")
        List<SiteProjectImageReq> images,
        @Schema(description = "ID навыков из справочника; полная замена списка при создании")
        List<@Positive Long> skillIds,
        @Schema(description = "Показывать на `GET /public/vitrina/home`; если false — только в `POST /projects/filter` для авторизованных (плюс окно публикации)")
        boolean visibleToAnonymous,
        @Schema(description = "Нижняя граница публикации; null — без ограничения «не раньше»")
        LocalDateTime publishedFrom,
        @Schema(description = "Верхняя граница публикации; null — без ограничения «не позже»")
        LocalDateTime publishedTo
) {
}
