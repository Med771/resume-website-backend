package ru.ai.sin.logic.vacancy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PatchVacancyVitrinaReq")
public record PatchVacancyVitrinaReq(
        Boolean visibleToAnonymous,
        Integer manualSortOrder
) {
}
