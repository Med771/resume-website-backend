package ru.ai.sin.logic.vacancy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(name = "VacancyModerationRejectReq")
public record VacancyModerationRejectReq(
        @Size(max = 2000)
        String moderationRejectionReason
) {
}
