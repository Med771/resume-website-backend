package ru.ai.sin.logic.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "CreateStudentPortfolioReq", description = "DTO для создания записи портфолио студента")
public record CreateStudentPortfolioReq(
        @Schema(description = "Название проекта/портфолио")
        @NotBlank
        @Size(min = 1, max = 255, message = "Name must be less than 255 characters")
        String name,

        @Schema(description = "Ссылка на проект")
        @NotBlank
        @Size(min = 1, max = 255, message = "Link must be less than 255 characters")
        String link,

        @Schema(description = "Дополнительная информация")
        @Size(max = 2000, message = "Additional info must be less than 2000 characters")
        String additionalInfo
) {
}
