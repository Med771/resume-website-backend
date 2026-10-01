package ru.ai.sin.logic.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(name = "CreateStudentExperienceReq", description = "DTO для создания опыта работы студента")
public record CreateStudentExperienceReq(
        @Schema(description = "ID существующей компании")
        @Positive
        Long companyId,

        @Schema(description = "Название компании (если companyId не передан)")
        @Size(min = 1, max = 255, message = "Company name must be less than 255 characters")
        String companyName,

        @Schema(description = "Должность")
        @NotBlank
        @Size(min = 1, max = 255, message = "Position must be less than 255 characters")
        String position,

        @Schema(description = "Дополнительная информация")
        @Size(max = 2000, message = "Additional info must be less than 2000 characters")
        String additionalInfo,

        @Schema(description = "Дата начала работы")
        LocalDate startDate,
        @Schema(description = "Дата окончания работы")
        LocalDate endDate
) {
}
