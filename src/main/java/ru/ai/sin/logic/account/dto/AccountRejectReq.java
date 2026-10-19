package ru.ai.sin.logic.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(name = "AccountRejectReq")
public record AccountRejectReq(
        @Schema(description = "Код причины отказа студента: INCOMPLETE_PROFILE, INVALID_DATA, NOT_ELIGIBLE, DUPLICATE, OTHER")
        @Size(max = 64)
        String reasonCode,

        @Schema(description = "Комментарий. Обязателен для студента при reasonCode=OTHER")
        @Size(max = 2000)
        String comment
) {
}
