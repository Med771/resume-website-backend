package ru.ai.sin.models.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EducationType {
    STATE("STATE"),
    COURSE("COURSE");

    private final String code;

    public static EducationType fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return STATE;
        }
        String code = raw.trim().toUpperCase();
        for (EducationType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown educationType: " + raw);
    }
}
