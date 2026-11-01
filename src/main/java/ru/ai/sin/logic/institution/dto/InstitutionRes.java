package ru.ai.sin.logic.institution.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ru.ai.sin.models.enums.EducationType;

public record InstitutionRes(
        long id,

        @Min(1900)
        @Max(2100)
        int startYear,

        @Min(1900)
        @Max(2100)
        int endYear,

        @NotNull
        EducationType educationType) {
}
