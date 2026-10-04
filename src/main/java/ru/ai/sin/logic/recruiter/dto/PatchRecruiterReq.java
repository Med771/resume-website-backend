package ru.ai.sin.logic.recruiter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(
        name = "PatchRecruiterReq",
        description = "Частичное обновление рекрутера. В JSON указывайте только изменяемые поля; null — не менять")
public record PatchRecruiterReq(
        @Schema(description = "Название компании")
        @Size(min = 1, max = 255, message = "Company name must be less than 255 characters")
        String companyName,

        @Schema(description = "Город компании или ИП")
        @Size(max = 255, message = "City must be less than 255 characters")
        String city,

        @Schema(description = "Имя")
        @Size(min = 1, max = 255, message = "First name must be less than 255 characters")
        String firstName,

        @Schema(description = "Фамилия")
        @Size(min = 1, max = 255, message = "Last name must be less than 255 characters")
        String lastName,

        @Schema(description = "Email")
        @Email(message = "Email should be valid")
        String email,

        @Schema(description = "Телефон")
        @Pattern(regexp = "\\+?\\d{1,15}", message = "Phone number must contain 1-15 digits and optional + at start")
        String phoneNumber,

        @Schema(description = "Telegram username")
        @Size(min = 1, max = 32, message = "Telegram username must be less than 32 characters")
        String telegramUsername
) {
}
