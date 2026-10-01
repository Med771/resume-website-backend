package ru.ai.sin.logic.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "EntityPopulationSummaryDTO",
        description = """
                Сводка по количеству пользователей (по ролям), студентов и рекрутеров.
                Поля `newStudentsInWindow` / `newRecruitersInWindow` заполняются только если в запросе переданы оба `from` и `to`.""")
public record EntityPopulationSummaryDTO(
        @Schema(description = "Всего строк в `users`")
        long totalUsers,

        @Schema(description = "Пользователи с ролью RECRUITER")
        long usersRecruiter,
        @Schema(description = "Пользователи с ролью STUDENT")
        long usersStudent,
        @Schema(description = "Пользователи с ролью ADMIN")
        long usersAdmin,

        @Schema(description = "Всего карточек в `students`")
        long totalStudents,

        @Schema(description = "Всего записей в `recruiters`")
        long totalRecruiters,

        @Schema(description = "Студенты с `created_at` в [from, to); null если окно не запрашивали")
        Long newStudentsInWindow,

        @Schema(description = "Рекрутеры с `created_at` в [from, to); null если окно не запрашивали")
        Long newRecruitersInWindow
) {
}
