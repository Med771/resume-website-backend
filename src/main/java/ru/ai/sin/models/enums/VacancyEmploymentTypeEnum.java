package ru.ai.sin.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VacancyEmploymentTypeEnum {
    INTERNSHIP("internship"),
    PART_TIME("part_time"),
    FULL_TIME("full_time"),
    PROJECT("project");

    private final String code;

    public static VacancyEmploymentTypeEnum fromCode(String code) {
        for (VacancyEmploymentTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown employment type: " + code);
    }
}
