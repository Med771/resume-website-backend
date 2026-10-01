package ru.ai.sin.logic.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(
        name = "StudentAccountRegistrationReq",
        description = """
                Саморегистрация студента: сразу создаёт User и черновик карточки, выдаёт cookie.
                Код с почты подтверждается отдельно (`POST /auth/confirm-email`).
                `lastName` — фамилия (как в проде), `firstName` — имя, `middleName` — отчество.""")
public record StudentAccountRegistrationReq(
        @Schema(description = "Логин")
        @NotBlank
        @Size(min = 3, max = 64)
        @Pattern(regexp = "^[a-zA-Z0-9_]{3,64}$", message = "Username must be 3-64 characters, letters, digits or _")
        String username,

        @Schema(description = "Пароль")
        @NotBlank
        String password,

        @Schema(description = "Подтверждение пароля")
        @NotBlank
        String passwordConfirm,

        @Schema(description = "Имя")
        @Size(max = 255)
        String firstName,

        @Schema(description = "Фамилия")
        @Size(max = 255)
        String lastName,

        @Schema(description = "Отчество (необязательно)")
        @Size(max = 255)
        String middleName,

        @Schema(description = "Email — на него уйдёт код подтверждения")
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @Schema(description = "Город / кампус (необязательно)")
        @Size(max = 255)
        String city,

        @Schema(description = "Номер телефона (контакт, без Telegram-подтверждения)")
        @NotBlank
        @Pattern(regexp = "\\+?\\d{7,15}", message = "Phone number must contain 7-15 digits and optional + at start")
        String phoneNumber
) {
}
