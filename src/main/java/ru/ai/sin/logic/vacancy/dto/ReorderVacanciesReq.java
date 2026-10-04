package ru.ai.sin.logic.vacancy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

@Schema(name = "ReorderVacanciesReq")
public record ReorderVacanciesReq(
        @NotEmpty
        List<UUID> orderedIds
) {
}
