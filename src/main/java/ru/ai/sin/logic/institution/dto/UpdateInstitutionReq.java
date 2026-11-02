package ru.ai.sin.logic.institution.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import ru.ai.sin.models.enums.EducationType;

import java.util.UUID;

public record UpdateInstitutionReq(
        long educationId,
        UUID studentId,

        @Min(1900)
        @Max(2100)
        int startYear,

        @Min(1900)
        @Max(2100)
        int endYear,

        EducationType educationType,

        @Size(max = 255)
        String direction,

        @Size(max = 255)
        String educationLevel) {
}
