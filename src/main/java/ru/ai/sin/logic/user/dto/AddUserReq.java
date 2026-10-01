package ru.ai.sin.logic.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.ai.sin.models.enums.RoleEnum;

import java.util.UUID;

public record AddUserReq(
        @Size(max = 255)
        String name,

        @NotBlank
        @Size(min = 3, max = 64)
        @Pattern(regexp = "^[a-zA-Z0-9_]{3,64}$", message = "Username must be 3-64 characters, letters, digits or _")
        String username,

        @NotBlank
        @Size(min = 1, max = 128)
        String password,

        @Schema(description = "По умолчанию RECRUITER. Для STUDENT укажите studentId.")
        RoleEnum role,

        @Schema(description = "Обязателен при role=STUDENT; карточка студента не должна быть привязана к другому пользователю.")
        UUID studentId
) {
}
