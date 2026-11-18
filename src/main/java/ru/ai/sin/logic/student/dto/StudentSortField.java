package ru.ai.sin.logic.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        enumAsRef = true,
        description = """
                Разрешённые ключи сортировки для `FilterStudentReq`.
                Произвольные имена полей из query `sort=` **не** поддерживаются.""")
public enum StudentSortField {
    @Schema(description = """
            Релевантность: `profileTextScore` DESC, затем аватар (`imagePath` ASC), затем дата создания DESC.
            Ручной номер админа в этот порядок не входит.""")
    RELEVANCE,
    @Schema(description = "Время создания записи (`timestamps.createdAt`)")
    CREATED_AT,
    @Schema(description = "Фамилия (`userInformation.lastName`)")
    LAST_NAME,
    @Schema(description = "Email карточки (`userInformation.email`)")
    EMAIL,
    @Schema(description = "Курс (`course`)")
    COURSE,
    @Schema(description = """
            Статус аккаунта на `users`. Не поле студента: порядок задаёт спецификация,
            а не `Sort.by`. Без `sortDirection` и при ASC: PENDING_APPROVAL, APPROVED, REJECTED.
            DESC переворачивает только этот порядок. У остальных полей пустое направление — DESC.""")
    ACCOUNT_STATUS,
    @Schema(description = "Дата рождения")
    BIRTH_DATE,
    @Schema(description = "Денормализованная сумма длин текстовых полей профиля (обновляется при сохранении)")
    PROFILE_TEXT_SCORE,

    @Deprecated
    @Schema(deprecated = true, description = "Устарело. Явная сортировка по ручному номеру. Каталог сам по нему больше не сортирует.")
    MANUAL_SORT_ORDER
}
