package ru.ai.sin.logic.recruiter.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Отклонение заявки на регистрацию рекрутера")
public record RecruiterRegistrationRejectReq(
        @Size(max = 2000)
        String reason
) {
}
