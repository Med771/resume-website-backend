package ru.ai.sin.logic.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "ForgotPasswordReq", description = "Запрос кода для смены пароля")
public record ForgotPasswordReq(
        @NotBlank
        @Email(message = "Email should be valid")
        @Schema(description = "Почта аккаунта")
        String email
) {
}
