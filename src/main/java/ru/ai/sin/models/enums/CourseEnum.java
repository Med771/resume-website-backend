package ru.ai.sin.models.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Schema(description = "Курс обучения: 1–5 (`FIRST`…`FIFTH`)")
public enum CourseEnum {
    FIRST("1"),
    SECOND("2"),
    THIRD("3"),
    FOURTH("4"),
    FIFTH("5");

    private final String course;

    public static CourseEnum fromCourse(String course) {
        for (CourseEnum status : values()) {
            if (status.getCourse().equals(course)) return status;
        }
        throw new IllegalArgumentException("Unknown code: " + course);
    }
}
