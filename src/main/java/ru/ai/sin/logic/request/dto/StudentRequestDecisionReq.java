package ru.ai.sin.logic.request.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Решение студента по заявке")
public record StudentRequestDecisionReq(
        @NotNull
        @Schema(description = "true — принять, false — отклонить")
        Boolean accept,

        @Size(max = 4000)
        String comment
) {
}
