package ru.ai.sin.logic.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "AccountEmailVerifiedReq")
public record AccountEmailVerifiedReq(
        @Schema(description = "true — подтвердить почту студента, false — снять подтверждение")
        @NotNull
        Boolean emailVerified
) {
}
