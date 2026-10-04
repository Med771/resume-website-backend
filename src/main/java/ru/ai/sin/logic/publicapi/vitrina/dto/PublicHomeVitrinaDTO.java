package ru.ai.sin.logic.publicapi.vitrina.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.ai.sin.logic.siteproject.dto.SiteProjectDTO;
import ru.ai.sin.logic.student.dto.StudentCardDTO;

import java.util.List;

@Schema(description = "Агрегированная публичная витрина главной страницы: резюме и проекты")
public record PublicHomeVitrinaDTO(
        @Schema(description = "Карточки студентов для слайдера на главной")
        List<StudentCardDTO> students,
        @Schema(description = "Проекты для блока на главной")
        List<SiteProjectDTO> projects
) {
}
