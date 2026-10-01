package ru.ai.sin.logic.siteproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.ai.sin.models.enums.CourseEnum;

import java.util.UUID;

@Schema(
        name = "SiteProjectParticipantDTO",
        description = "Краткая карточка студента в составе проекта (без контактов).")
public record SiteProjectParticipantDTO(
        @Schema(description = "ID студента")
        UUID id,
        @Schema(description = "Имя")
        String firstName,
        @Schema(description = "Фамилия")
        String lastName,
        @Schema(description = "Путь к фото профиля в хранилище приложения")
        String imagePath,
        @Schema(description = "Специальность")
        String speciality,
        @Schema(description = "Курс")
        CourseEnum course
) {
}
