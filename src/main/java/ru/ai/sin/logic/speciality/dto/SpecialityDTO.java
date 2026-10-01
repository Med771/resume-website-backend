package ru.ai.sin.logic.speciality.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SpecialityDTO(
        long id,

        @NotBlank
        @Size(min = 1, max = 255, message = "Name must be less than 255 characters")
        String name,

        String iconPath
) { }
