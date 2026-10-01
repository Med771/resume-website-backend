package ru.ai.sin.logic.request.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(
        description = "Создание заявки. Если у текущего пользователя уже привязан профиль рекрутера (см. GET /recruiter/me), "
                + "достаточно передать только studentId; поля компании и контактов не нужны.")
public record AddRequestReq(
        @Schema(description = "Обязательно, пока к аккаунту не привязан рекрутер")
        @Size(min = 1, max = 255, message = "Company name must be less than 255 characters")
        String companyName,

        @Size(min = 1, max = 255, message = "First name must be less than 255 characters")
        String firstName,

        @Size(min = 1, max = 255, message = "Last name must be less than 255 characters")
        String lastName,

        @Email(message = "Email should be valid")
        String email,

        @Pattern(regexp = "\\+?\\d{1,15}", message = "Phone number must contain 1-15 digits and optional + at start")
        String phoneNumber,

        @Size(min = 1, max = 32, message = "Telegram username must be less than 32 characters")
        String telegramUsername,

        @Schema(description = "ID студента, на которого подаётся заявка", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        UUID studentId
) {
}

