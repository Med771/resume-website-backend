package ru.ai.sin.logic.recruitment.dto;

import ru.ai.sin.models.enums.CooperationFormat;
import ru.ai.sin.models.enums.RecruitmentRequestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RecruitmentRequestDTO(
        UUID id,
        UUID recruiterId,
        String companyName,
        String tasks,
        String qualificationRequirements,
        String workConditions,
        int studentCount,
        CooperationFormat cooperationFormat,
        Boolean readyToRequestStudents,
        String scheduleNote,
        String extraInfo,
        LocalDateTime publishedFrom,
        LocalDateTime publishedTo,
        RecruitmentRequestStatus status,
        List<Long> specialityIds,
        List<Long> cityIds
) {
}
