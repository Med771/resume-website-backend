package ru.ai.sin.logic.vacancy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.ai.sin.models.enums.VacancyStatus;

import java.util.UUID;

@Schema(name = "FilterVacancyModerationReq")
public record FilterVacancyModerationReq(
        @Schema(description = "По умолчанию PENDING_REVIEW")
        VacancyStatus status,
        String findString,
        UUID recruiterId,
        String companyName
) {
}
