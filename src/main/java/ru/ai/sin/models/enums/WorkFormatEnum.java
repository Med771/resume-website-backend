package ru.ai.sin.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WorkFormatEnum {
    REMOTE("remote"),
    OFFICE("office"),
    HYBRID("hybrid");

    private final String code;

    public static WorkFormatEnum fromCode(String code) {
        for (WorkFormatEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown work format: " + code);
    }
}
