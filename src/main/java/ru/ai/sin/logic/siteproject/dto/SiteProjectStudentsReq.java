package ru.ai.sin.logic.siteproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

@Schema(
        name = "SiteProjectStudentsReq",
        description = "Список UUID студентов для привязки или отвязки (`POST`/`DELETE /projects/{id}/students`), только ADMIN.")
public record SiteProjectStudentsReq(
        @NotEmpty
        @Schema(description = "UUID студентов; дубликаты в списке запрещены", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull UUID> studentIds
) {
}
