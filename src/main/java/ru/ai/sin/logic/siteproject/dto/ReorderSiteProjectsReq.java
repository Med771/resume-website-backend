package ru.ai.sin.logic.siteproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

@Schema(
        name = "ReorderSiteProjectsReq",
        description = """
                Тело `POST /projects/reorder`: задаёт новые значения `sortOrder` для перечисленных проектов.
                Порядок в массиве `orderedIds` определяет индекс сортировки (0 — первый).""")
public record ReorderSiteProjectsReq(
        @NotEmpty
        @Schema(description = "Список UUID проектов в желаемом порядке; каждый id должен существовать, без дубликатов")
        List<UUID> orderedIds
) {
}
