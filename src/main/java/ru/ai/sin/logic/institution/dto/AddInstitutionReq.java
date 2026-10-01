package ru.ai.sin.logic.institution.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

public record AddInstitutionReq(
        long educationId,
        UUID studentId,

        @Min(1900)
        @Max(2100)
        int startYear,

        @Min(1900)
        @Max(2100)
        int endYear) {
}
