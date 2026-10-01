package ru.ai.sin.logic.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        enumAsRef = true,
        description = """
                Разрешённые ключи сортировки для `FilterStudentReq`.
                Произвольные имена полей из query `sort=` **не** поддерживаются.""")
public enum StudentSortField {
    @Schema(description = """
            Релевантность: сначала ручной номер `manualSortOrder` ASC (NULL в конце на PostgreSQL),
            затем аватар (`imagePath` ASC), затем `profileTextScore` DESC, затем дата создания DESC.""")
    RELEVANCE,
    @Schema(description = "Время создания записи (`timestamps.createdAt`)")
    CREATED_AT,
    @Schema(description = "Фамилия (`userInformation.lastName`)")
    LAST_NAME,
    @Schema(description = "Дата рождения")
    BIRTH_DATE,
    @Schema(description = "Денормализованная сумма длин текстовых полей профиля (обновляется при сохранении)")
    PROFILE_TEXT_SCORE,

    @Schema(description = "Ручной номер в каталоге (`manualSortOrder`): меньше — выше при ASC, NULL — в конце при ASC (PostgreSQL)")
    MANUAL_SORT_ORDER
}
