package ru.ai.sin.logic.vacancy.dto;

import ru.ai.sin.logic.skill.dto.SkillDTO;
import ru.ai.sin.models.enums.VacancyEmploymentTypeEnum;
import ru.ai.sin.models.enums.VacancyStatus;
import ru.ai.sin.models.enums.WorkFormatEnum;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VacancyDTO(
        UUID id,
        UUID recruiterId,
        String title,
        String description,
        String companyName,
        String city,
        WorkFormatEnum workFormat,
        VacancyEmploymentTypeEnum employmentType,
        Long specialityId,
        String specialityName,
        List<SkillDTO> skills,
        VacancyStatus status,
        LocalDateTime publishedFrom,
        LocalDateTime publishedTo,
        Integer slotsCount,
        LocalDateTime submittedForReviewAt,
        LocalDateTime moderatedAt,
        String moderatedByUsername,
        String moderationRejectionReason,
        long applicationsCount,
        Boolean hasApplied,
        LocalDateTime createdAt,
        Integer manualSortOrder,
        boolean visibleToAnonymous
) {
}
