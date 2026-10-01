package ru.ai.sin.logic.recruiter.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddRecruiterReq(
        @NotBlank
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
        String telegramUsername
) {
}
