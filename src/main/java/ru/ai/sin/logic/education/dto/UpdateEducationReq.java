package ru.ai.sin.logic.education.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateEducationReq(
        @NotBlank
        @Size(min = 1, max = 255, message = "Institution must be less than 2000 characters")
        String institution,

        @NotBlank
        @Size(min = 1, max = 2000, message = "Additional info must be less than 2000 characters")
        String additionalInfo,

        @NotBlank
        @Size(min = 1, max = 255, message = "Web Url must be less than 2000 characters")
        String webUrl) {
}
