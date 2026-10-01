package ru.ai.sin.logic.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(name = "PhoneVerificationStartReq")
public record PhoneVerificationStartReq(
        @NotBlank
        @Pattern(regexp = "\\+?\\d{7,15}", message = "Phone number must contain 7-15 digits and optional + at start")
        String phoneNumber,
        @Email
        @Schema(description = "Почта для отправки OTP-кода (опционально; при app.mail.enabled=true)")
        String email
) {
}
