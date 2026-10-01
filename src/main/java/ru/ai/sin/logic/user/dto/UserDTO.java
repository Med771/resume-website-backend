package ru.ai.sin.logic.user.dto;

import jakarta.validation.constraints.NotNull;
import ru.ai.sin.models.enums.RoleEnum;

import java.util.UUID;

public record UserDTO(
        @NotNull
        UUID id,

        String name,

        @NotNull
        String username,

        @NotNull
        RoleEnum role,

        UUID recruiterId,

        UUID studentId
) {
}
