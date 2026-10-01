package ru.ai.sin.logic.siteproject.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import ru.ai.sin.logic.skill.dto.SkillDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(
        name = "SiteProjectDTO",
        description = """
                Проект ленты. Галерея — `images` (файл в хранилище и/или внешняя ссылка).
                Поле `students` заполняется для рекрутера и админа; для анонима и студента — null.
                Навыки (`skills`) возвращаются всем ролям.""")
public record SiteProjectDTO(
        @Schema(description = "UUID записи", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,
        @Schema(description = "Заголовок")
        String title,
        @Schema(description = "Раздел / категория проекта")
        String section,
        @Schema(description = "Краткое описание")
        String summary,
        @Schema(description = "Полный текст")
        String body,
        @Schema(description = "Галерея изображений в порядке sortOrder")
        List<SiteProjectImageDTO> images,
        @Schema(description = "Навыки / технологии проекта")
        List<SkillDTO> skills,
        @Schema(description = "Порядок сортировки (меньше — выше в списке при одинаковых условиях фильтра)")
        int sortOrder,
        @Schema(description = "Показывать на анонимной витрине `GET /public/vitrina/home`; при false — только в `POST /projects/filter` для авторизованных (плюс окно публикации)")
        boolean visibleToAnonymous,
        @Schema(description = "Начало окна публикации; null — без нижней границы")
        LocalDateTime publishedFrom,
        @Schema(description = "Конец окна публикации; null — без верхней границы")
        LocalDateTime publishedTo,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "Участники проекта; null — поле скрыто для текущей роли")
        List<SiteProjectParticipantDTO> students
) {
}
