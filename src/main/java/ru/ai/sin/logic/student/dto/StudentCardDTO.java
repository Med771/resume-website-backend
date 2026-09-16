package ru.ai.sin.logic.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.ai.sin.logic.skill.dto.SkillDTO;
import ru.ai.sin.models.enums.CourseEnum;

import java.util.List;
import java.util.UUID;

@Schema(
        name = "StudentCardDTO",
        description = """
                Краткая карточка для списков и публичной витрины.
                Не содержит контактов (email, телефон и т.д.) — намеренно, чтобы снизить утечку PII на анонимных страницах.""")
public record StudentCardDTO(
        @Schema(description = "ID студента")
        @NotNull
        UUID id,

        @Schema(description = "Краткая информация")
        @Size(max = 2000, message = "Additional info must be less than 2000 characters")
        String bio,

        @Schema(description = "Имя")
        String firstName,
        @Schema(description = "Фамилия")
        String lastName,
        @Schema(description = "Отчество")
        String middleName,

        @Schema(description = "Путь к изображению профиля")
        String imagePath,

        @Schema(description = "Текущий курс (1–5)")
        @NotNull
        CourseEnum course,

        @Schema(description = "Название специальности")
        @NotNull
        String speciality,

        @Schema(description = "Навыки студента")
        @NotNull
        List<SkillDTO> skills,

        @Schema(description = "Ручной приоритет в каталоге (см. `StudentDTO.manualSortOrder`); для анонимной витрины может использоваться в сортировке")
        Integer manualSortOrder) {
}
