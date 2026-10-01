package ru.ai.sin.logic.vacancy.dto;

import ru.ai.sin.models.enums.VacancyEmploymentTypeEnum;
import ru.ai.sin.models.enums.WorkFormatEnum;

import java.time.LocalDateTime;
import java.util.UUID;

public record VacancyCardDTO(
        UUID id,
        String title,
        String summary,
        String companyName,
        String city,
        WorkFormatEnum workFormat,
        VacancyEmploymentTypeEnum employmentType,
        String specialityName,
        long applicationsCount,
        Boolean hasApplied,
        LocalDateTime publishedFrom
) {
}
