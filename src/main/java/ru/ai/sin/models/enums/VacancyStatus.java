package ru.ai.sin.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VacancyStatus {
    DRAFT("draft"),
    PENDING_REVIEW("pending_review"),
    PUBLISHED("published"),
    REJECTED("rejected"),
    CLOSED("closed"),
    ARCHIVED("archived");

    private final String code;

    public static VacancyStatus fromCode(String code) {
        for (VacancyStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown vacancy status: " + code);
    }
}
