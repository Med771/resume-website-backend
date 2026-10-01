package ru.ai.sin.logic.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordReq(
        @NotBlank String currentPassword,
        @NotBlank String newPassword
) {
}
