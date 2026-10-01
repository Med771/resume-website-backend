package ru.ai.sin.logic.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BulkStudentVisibilityResult")
public record BulkStudentVisibilityResult(
        @Schema(description = "Сколько карточек реально изменено")
        int updatedCount,
        @Schema(description = "Сколько карточек попало в выборку")
        int matchedCount
) {
}
