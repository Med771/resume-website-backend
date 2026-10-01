package ru.ai.sin.logic.recruiter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RecruiterRes(
        @NotNull
        UUID id,

        @NotBlank
        @Size(min = 1, max = 255, message = "Company name must be less than 255 characters")
        String companyName,

        @Size(min = 1, max = 255, message = "First name must be less than 255 characters")
        String fullName,

        @NotNull
        String chatId
) {
}
