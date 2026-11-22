package ru.ai.sin.logic.recruitment.dto;

import ru.ai.sin.models.enums.RecruitmentResponseStatus;

import java.util.UUID;

public record RecruitmentResponseDTO(
        UUID id,
        UUID recruitmentRequestId,
        UUID studentId,
        String firstName,
        String lastName,
        String speciality,
        RecruitmentResponseStatus status
) {
}
