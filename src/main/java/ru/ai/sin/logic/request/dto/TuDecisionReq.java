package ru.ai.sin.logic.request.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(name = "TuDecisionReq")
public record TuDecisionReq(
        @Schema(description = "true — подтвердить ТУ, false — отказ")
        Boolean accept,
        @Schema(description = "Код причины отказа (NOT_A_FIT, NO_RESPONSE, …)")
        String reasonCode,
        @Size(max = 2000) String comment
) {
}
