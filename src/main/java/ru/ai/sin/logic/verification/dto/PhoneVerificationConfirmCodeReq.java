package ru.ai.sin.logic.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "PhoneVerificationConfirmCodeReq", description = "Подтверждение номера тестовым кодом (только при app.telegram.allow-dev-confirm=true)")
public record PhoneVerificationConfirmCodeReq(
        @Schema(description = "Код подтверждения")
        @NotBlank
        @Size(min = 4, max = 8)
        String code
) {
}
