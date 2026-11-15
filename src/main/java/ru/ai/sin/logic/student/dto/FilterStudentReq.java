package ru.ai.sin.logic.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import ru.ai.sin.models.enums.BusynessEnum;
import ru.ai.sin.models.enums.CourseEnum;

import java.time.LocalDate;

import java.util.List;
import java.util.Set;

@Schema(
        name = "FilterStudentReq",
        description = """
                Фильтры списка студентов для `POST /student/cardsFilter` и `POST /student/filter`.

                **Сортировка:** только поля ниже; query-параметр `sort` у `Pageable` на сервере **не используется** (белый список полей).

                **useDefaultRanking:** если `null` или `true`, и `sortBy` не задан или равен `RELEVANCE`.
                Студент и работодатель без строки поиска видят порядок: статус поиска (`looking`, `considering`, затем `not_looking` и пусто),
                внутри него желаемый формат (`employed`, `freelance`, `project_work`, затем `free` и пусто).
                Админ и поиск по строке: `profileTextScore` DESC, затем `imagePath` ASC, затем дата создания DESC.
                Ручной номер админа устарел и в этот порядок не входит.

                Если `useDefaultRanking=false`, используется `sortBy` (по умолчанию `CREATED_AT`, если null) и `sortDirection` (по умолчанию DESC).""")
public record FilterStudentReq(
        @Schema(description = """
                Подстрока. Студент и работодатель ищут её в фамилии или имени.
                Админ ищет в склейке «имя фамилия отчество», а если никого нет — в описании.""")
        String findString,

        @Schema(description = "Ограничение по одному или нескольким значениям курса (`CourseEnum`)")
        Set<CourseEnum> course,

        @Schema(description = "Ограничение по типу занятости (`BusynessEnum`)")
        Set<BusynessEnum> busyness,

        @Schema(description = "Дата рождения ≤ указанной")
        LocalDate bornBefore,

        @Schema(description = "Дата рождения ≥ указанной")
        LocalDate bornAfter,

        @Schema(description = "Студент должен иметь **все** перечисленные навыки (пересечение по id)")
        List<@Positive Long> skillsIds,

        @Schema(description = "Студент должен относиться к одной из перечисленных специальностей")
        List<@Positive Long> specialitiesIds,

        @Schema(description = "Поле сортировки из разрешённого перечисления `StudentSortField`")
        StudentSortField sortBy,

        @Schema(description = """
                Направление для явной сортировки (`useDefaultRanking=false`); при null — DESC.
                Для `sortBy=ACCOUNT_STATUS` пустое направление значит «сначала ожидающие» (ASC):
                PENDING_APPROVAL, APPROVED, REJECTED.""")
        StudentSortDirection sortDirection,

        @Schema(description = """
                `null` или `true` — режим релевантности при `sortBy=null` или `RELEVANCE` (ручной номер → score текста → аватар → дата создания).
                `false` — явная сортировка по `sortBy` / `sortDirection`.""")
        Boolean useDefaultRanking,

        @Schema(description = "Фильтр по видимости в каталоге рекрутёров; `null` — без ограничения")
        Boolean catalogVisible,

        @Schema(description = "Фильтр по согласию на публичную витрину; `null` — без ограничения")
        Boolean publicProfileConsent) {
}
