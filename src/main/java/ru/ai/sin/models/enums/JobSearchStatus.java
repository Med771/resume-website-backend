package ru.ai.sin.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum JobSearchStatus {
    LOOKING("looking"),
    CONSIDERING("considering"),
    NOT_LOOKING("not_looking");

    private final String code;

    public static JobSearchStatus fromCode(String code) {
        for (JobSearchStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}
