package ru.ai.sin.logic.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(name = "ConfirmEmailReq", description = "Код из письма после POST /auth/register-student")
public record ConfirmEmailReq(
        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "Код должен состоять из 6 цифр")
        @Schema(description = "Шестизначный код")
        String code
) {
}
