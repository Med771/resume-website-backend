package ru.ai.sin.models.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SyncTypeEnum {
    RE("re"),
    ST("st");

    @JsonValue
    private final String code;

    public static SyncTypeEnum fromCode(String code) {
        for (SyncTypeEnum t : values()) {
            if (t.code.equals(code)) return t;
        }
        throw new IllegalArgumentException("Unknown sync type: " + code);
    }
}
