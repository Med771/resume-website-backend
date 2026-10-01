package ru.ai.sin.logic.recruiter.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.UUID;

@Schema(name = "RecruiterSelfRegistrationReq", description = "Заявка на регистрацию работодателя; вход после одобрения администратором")
public record RecruiterSelfRegistrationReq(
        @Schema(description = "Логин будущего аккаунта")
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

        @Schema(description = "Отображаемое имя (необязательно, иначе собирается из ФИО)")
        @Size(max = 255)
        String name,

        @Schema(description = "Название компании (можно заполнить позже в профиле)")
        @Size(max = 255)
        String companyName,

        @Schema(description = "Город компании или ИП")
        @Size(max = 255)
        String city,

        @Schema(description = "Имя")
        @Size(min = 1, max = 255)
        String firstName,

        @Schema(description = "Фамилия")
        @Size(min = 1, max = 255)
        String lastName,

        @Schema(description = "Отчество (необязательно)")
        @Size(max = 255)
        String middleName,

        @Schema(description = "Email компании/контакта (можно заполнить позже в профиле)")
        @Email
        @Size(max = 255)
        String email,

        @Schema(description = "Телефон")
        @Pattern(regexp = "\\+?\\d{1,15}", message = "Phone number must contain 1-15 digits and optional + at start")
        String phoneNumber,

        @Schema(description = "Telegram username")
        @Size(min = 1, max = 32)
        String telegramUsername,

        @Schema(description = "ID сессии верификации телефона в Telegram")
        @NotNull
        UUID phoneVerificationId,

        @Schema(description = "Согласие на рассылку советов и предложений")
        Boolean marketingConsent
) {
}
