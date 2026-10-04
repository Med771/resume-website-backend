package ru.ai.sin.logic.speciality.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record UpdateSpecialityReq(
        @NotBlank
        @Size(min = 1, max = 255, message = "Name must be less than 255 characters")
        String name,
        @Size(max = 512)
        String iconPath) {
}
