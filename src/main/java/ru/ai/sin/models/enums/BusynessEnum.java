package ru.ai.sin.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BusynessEnum {
    FREE("free"),
    FREELANCE("freelance"),
    EMPLOYED("employed");


    private final String busyness;

    public static BusynessEnum fromBusyness(String busyness) {
        for (BusynessEnum status: values()) {
            if (status.getBusyness().equals(busyness)) return status;
        }
        throw new IllegalArgumentException("Unknown code: " + busyness);

    }
}
