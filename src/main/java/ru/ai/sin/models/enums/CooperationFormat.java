package ru.ai.sin.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CooperationFormat {
    SEMESTER_PROJECT("SEMESTER_PROJECT"),
    SUMMER_PRACTICE("SUMMER_PRACTICE"),
    INTERNSHIP("INTERNSHIP"),
    VACANCY("VACANCY"),
    OTHER("OTHER");

    private final String code;

    public static CooperationFormat fromCode(String code) {
        for (CooperationFormat format : values()) {
            if (format.getCode().equals(code)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown cooperation format: " + code);
    }
}
