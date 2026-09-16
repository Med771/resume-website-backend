package ru.ai.sin.models.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Schema(description = "Пол: `MALE` / `FEMALE`; в API `null` — не указан")
public enum GenderEnum {
    MALE("MALE"),
    FEMALE("FEMALE");

    private final String gender;

    public static GenderEnum fromGender(String gender) {
        if (gender == null || gender.isBlank()) {
            return null;
        }
        for (GenderEnum value : values()) {
            if (value.getGender().equalsIgnoreCase(gender.trim())) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown gender: " + gender);
    }
}
