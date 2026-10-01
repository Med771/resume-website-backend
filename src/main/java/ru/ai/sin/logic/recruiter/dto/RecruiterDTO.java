package ru.ai.sin.logic.recruiter.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record RecruiterDTO(
        @NotNull
        UUID id,

        @NotBlank
        @Size(min = 1, max = 255, message = "Company name must be less than 255 characters")
        String companyName,

        @Size(max = 255, message = "City must be less than 255 characters")
        String city,

        @Size(min = 1, max = 255, message = "First name must be less than 255 characters")
        String firstName,

        @Size(min = 1, max = 255, message = "Last name must be less than 255 characters")
        String lastName,

        @Pattern(regexp = "^[a-zA-Z0-9_]{3,64}$", message = "Username must be 3-64 characters, letters, digits or _")
        String username,

        @Email(message = "Email should be valid")
        String email,

        @Pattern(regexp = "\\+?\\d{1,15}", message = "Phone number must contain 1-15 digits and optional + at start")
        String phoneNumber,

        @Size(min = 1, max = 32, message = "Telegram username must be less than 32 characters")
        String telegramUsername,

        @Size(min = 1, max = 32, message = "Telegram user id must be less than 32 characters")
        String telegramUserId) {
}
