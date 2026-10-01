package ru.ai.sin.logic.recruiter.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.ai.sin.models.enums.RecruiterRegistrationStatus;

@Schema(description = "Фильтр заявок на регистрацию рекрутера")
public record FilterRecruiterRegistrationReq(
        @Schema(description = "Если null — все статусы")
        RecruiterRegistrationStatus status,

        @Schema(description = "Поиск по логину, компании, email, ФИО")
        String search
) {
}
