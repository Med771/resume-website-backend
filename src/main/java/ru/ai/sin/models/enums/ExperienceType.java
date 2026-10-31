package ru.ai.sin.models.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExperienceType {
    WORK("WORK"),
    PROJECT("PROJECT");

    private final String code;

    public static ExperienceType fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return WORK;
        }
        String code = raw.trim().toUpperCase();
        for (ExperienceType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown experienceType: " + raw);
    }
}
