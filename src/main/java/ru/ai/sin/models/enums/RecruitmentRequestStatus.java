package ru.ai.sin.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RecruitmentRequestStatus {
    OPEN("OPEN"),
    CLOSED("CLOSED"),
    ARCHIVED("ARCHIVED");

    private final String code;

    public static RecruitmentRequestStatus fromCode(String code) {
        for (RecruitmentRequestStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown recruitment request status: " + code);
    }
}
