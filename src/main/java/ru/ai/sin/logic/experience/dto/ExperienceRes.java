package ru.ai.sin.logic.experience.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ExperienceRes(
        long id,

        @NotBlank
        @Size(min = 1, max = 255, message = "Position must be less than 255 characters")
        String position,

        @Size(max = 2000, message = "Additional info must be less than 2000 characters")
        String additionalInfo,

        @NotNull
        LocalDate startDate,

        LocalDate endDate) {
}
