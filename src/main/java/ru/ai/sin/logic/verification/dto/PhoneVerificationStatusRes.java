package ru.ai.sin.logic.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.ai.sin.models.enums.PhoneVerificationStatus;

import java.util.UUID;

@Schema(name = "PhoneVerificationStatusRes")
public record PhoneVerificationStatusRes(
        UUID verificationId,
        PhoneVerificationStatus status
) {
}
