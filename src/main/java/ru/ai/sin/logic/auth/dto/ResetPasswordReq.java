package ru.ai.sin.logic.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(name = "ResetPasswordReq", description = "Новый пароль по коду из письма")
public record ResetPasswordReq(
        @NotBlank
        @Email(message = "Email should be valid")
        @Schema(description = "Почта аккаунта")
        String email,
        @NotBlank
        @Pattern(regexp = "\\d{4}", message = "Код должен состоять из 4 цифр")
        @Schema(description = "Четырёхзначный код")
        String code,
        @NotBlank
        String newPassword,
        @NotBlank
        String passwordConfirm
) {
}
