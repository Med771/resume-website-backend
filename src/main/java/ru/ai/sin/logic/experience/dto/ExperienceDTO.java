package ru.ai.sin.logic.experience.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ExperienceDTO(
        long companyId,

        @NotNull
        UUID studentId,

        @NotNull
        ExperienceRes experience) {
}
