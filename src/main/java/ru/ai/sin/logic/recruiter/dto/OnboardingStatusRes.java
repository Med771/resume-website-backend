package ru.ai.sin.logic.recruiter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RecruiterOnboardingStatusRes")
public record OnboardingStatusRes(
        @Schema(description = "Профиль рекрутера заполнен (компания, ФИО, email, город)")
        boolean profileCompleted,
        @Schema(description = "Создана хотя бы одна вакансия")
        boolean vacancyCompleted,
        @Schema(description = "Онбординг завершён (профиль рекрутёра заполнен; вакансия не обязательна)")
        boolean completed
) {
}
