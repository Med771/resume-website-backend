package ru.ai.sin.logic.education.dto;

import jakarta.validation.constraints.Size;

import java.util.Set;

public record FilterEducationReq(
        Set<Long> ids,

        @Size(min = 1, max = 255, message = "Institution must be less than 2000 characters")
        String institution,
        @Size(min = 1, max = 2000, message = "Additional info must be less than 2000 characters")
        String additionalInfo,

        @Size(min = 1, max = 255, message = "Web Url must be less than 2000 characters")
        String webUrl
) {
}
