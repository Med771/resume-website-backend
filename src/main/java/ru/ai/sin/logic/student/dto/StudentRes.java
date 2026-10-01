package ru.ai.sin.logic.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(name = "StudentRes", description = "Короткое представление студента для интеграций")
public record StudentRes(
        @Schema(description = "ID студента")
        @NotNull
        UUID id,

        @Schema(description = "Название специальности")
        @NotNull
        String speciality,

        @Schema(description = "Полное имя")
        @Size(min = 1, max = 255, message = "First name must be less than 255 characters")
        String fullName,

        @Schema(description = "Идентификатор чата")
        @NotNull
        String chatId
) {
}
