package ru.ai.sin.logic.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "AuthMeDTO", description = "Текущая сессия (роль, логин, id для WS inbox).")
public record AuthMeDTO(
        @Schema(description = "UUID учётной записи (`users.id`); для подписки `/topic/users/{id}/inbox`")
        UUID id,

        @Schema(description = "Логин пользователя")
        String username,
        @Schema(description = "Роль: STUDENT, RECRUITER, ADMIN")
        String role,
        @Schema(description = "Статус аккаунта: PENDING_APPROVAL, APPROVED, REJECTED")
        String accountStatus,
        @Schema(description = "Почта подтверждена кодом после регистрации (для студентов после нового флоу)")
        boolean emailVerified,
        @Schema(description = "Подсказки при создании резюме/вакансий отключены")
        boolean hintsDisabled
) {
}
