package ru.ai.sin.logic.student.dto;

import java.time.LocalDateTime;

public record StudentProfileVersionItemDTO(
        int versionNumber,
        String status,
        LocalDateTime createdAt
) {
}
