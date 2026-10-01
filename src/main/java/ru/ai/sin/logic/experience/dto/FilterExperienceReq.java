package ru.ai.sin.logic.experience.dto;

import java.util.UUID;

public record FilterExperienceReq(
        UUID studentId,
        Long companyId) {
}
