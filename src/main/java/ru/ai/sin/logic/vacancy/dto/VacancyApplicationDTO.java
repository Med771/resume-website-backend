package ru.ai.sin.logic.vacancy.dto;

import ru.ai.sin.logic.student.dto.StudentCardDTO;
import ru.ai.sin.models.enums.TuPhase;
import ru.ai.sin.models.enums.VacancyApplicationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record VacancyApplicationDTO(
        UUID id,
        UUID vacancyId,
        String vacancyTitle,
        UUID studentId,
        StudentCardDTO studentCard,
        VacancyApplicationStatus status,
        String coverLetter,
        String rejectionReason,
        UUID appChatId,
        LocalDateTime createdAt,
        LocalDateTime studentTuConfirmedAt,
        LocalDateTime recruiterTuConfirmedAt,
        String rejectionReasonCode,
        String rejectionComment,
        TuPhase tuPhase
) {
}
