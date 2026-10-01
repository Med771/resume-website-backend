package ru.ai.sin.logic.institution.dto;

import java.util.UUID;

public record FilterInstitutionReq(
        UUID studentId,
        Long educationId) {
}
