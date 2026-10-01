package ru.ai.sin.logic.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(name = "CreateStudentInstitutionReq", description = "DTO для создания записи об обучении студента")
public record CreateStudentInstitutionReq(
        @Schema(description = "ID существующего education")
        @Positive
        Long educationId,

        @Schema(description = "Название учебного заведения (если educationId не передан)")
        @Size(min = 1, max = 255, message = "Institution must be less than 255 characters")
        String institution,

        @Schema(description = "Дополнительная информация об обучении")
        @Size(max = 2000, message = "Additional info must be less than 2000 characters")
        String additionalInfo,

        @Schema(description = "Сайт учебного заведения")
        @Size(min = 1, max = 255, message = "Web Url must be less than 255 characters")
        String webUrl,

        @Schema(description = "Год начала обучения")
        @Min(1900)
        @Max(2100)
        int startYear,

        @Schema(description = "Год окончания обучения")
        @Min(1900)
        @Max(2100)
        int endYear
) {
}
