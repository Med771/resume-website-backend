package ru.ai.sin.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VacancyApplicationStatus {
    SUBMITTED("submitted"),
    ACCEPTED("accepted"),
    REJECTED("rejected"),
    WITHDRAWN("withdrawn");

    private final String code;

    public static VacancyApplicationStatus fromCode(String code) {
        for (VacancyApplicationStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown vacancy application status: " + code);
    }
}
