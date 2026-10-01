package ru.ai.sin.logic.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CompanyDTO(
        long id,

        @NotBlank
        @Size(min = 1, max = 255, message = "Name must be less than 255 characters")
        String name,

        @NotNull
        List<Long> experiencesIds) {
}
