package ru.ai.sin.logic.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

@Schema(name = "ReorderStudentsReq")
public record ReorderStudentsReq(
        @NotEmpty
        List<UUID> orderedIds
) {
}
