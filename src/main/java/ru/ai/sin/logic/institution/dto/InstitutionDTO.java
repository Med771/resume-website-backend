package ru.ai.sin.logic.institution.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InstitutionDTO(
        long educationId,

        @NotNull
        UUID studentId,

        @NotNull
        InstitutionRes institution) {
}
