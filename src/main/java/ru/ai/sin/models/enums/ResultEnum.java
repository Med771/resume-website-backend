package ru.ai.sin.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ResultEnum {
    CREATION("creation"),
    SYNC("sync"),
    WAITING("waiting"),
    EXPECTATION("expectation"),
    STUDENT_CONFIRMED("student_confirm"),
    RECRUITER_CONFIRMED("recruiter_conf"),
    SUCCESS("success"),
    REFUSAL("refusal");

    private final String result;

    public static ResultEnum fromCourse(String result) {
        for (ResultEnum status : values()) {
            if (status.getResult().equals(result)) return status;
        }
        throw new IllegalArgumentException("Unknown code: " + result);
    }
}
