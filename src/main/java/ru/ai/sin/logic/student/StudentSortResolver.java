package ru.ai.sin.logic.student;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;
import ru.ai.sin.logic.student.dto.FilterStudentReq;
import ru.ai.sin.logic.student.dto.StudentSortDirection;
import ru.ai.sin.logic.student.dto.StudentSortField;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StudentSortResolver {

    /**
     * Собирает {@link Sort} только из белого списка; произвольный {@code sort} из query string не используется.
     */
    public static Sort resolve(FilterStudentReq filter) {
        return resolve(filter, false);
    }

    /**
     * Для каталога студента и работодателя обычная выдача без строки поиска сортируется спецификацией.
     * Пустой {@link Sort} нужен, чтобы Spring Data не затёр этот порядок.
     */
    public static Sort resolve(FilterStudentReq filter, boolean catalogAudience) {
        if (catalogAudience && isCatalogRank(filter)) {
            return Sort.unsorted();
        }
        return resolveExplicit(filter);
    }

    private static Sort resolveExplicit(FilterStudentReq filter) {
        boolean useRanking = filter.useDefaultRanking() == null || filter.useDefaultRanking();
        StudentSortField field = filter.sortBy();
        if (useRanking && (field == null || field == StudentSortField.RELEVANCE)) {
            return defaultRelevanceSort();
        }
        StudentSortField effective = field == null ? StudentSortField.CREATED_AT : field;
        if (effective == StudentSortField.RELEVANCE) {
            effective = StudentSortField.CREATED_AT;
        }
        Sort.Direction dir = toSpringDirection(filter.sortDirection());
        return switch (effective) {
            case CREATED_AT -> Sort.by(new Sort.Order(dir, "timestamps.createdAt"));
            case LAST_NAME -> Sort.by(new Sort.Order(dir, "userInformation.lastName"));
            case EMAIL -> Sort.by(new Sort.Order(dir, "userInformation.email"));
            case COURSE -> Sort.by(new Sort.Order(dir, "course"));
            case BIRTH_DATE -> Sort.by(new Sort.Order(dir, "birthDate"));
            case PROFILE_TEXT_SCORE -> Sort.by(new Sort.Order(dir, "profileTextScore"));
            case MANUAL_SORT_ORDER -> Sort.by(new Sort.Order(dir, "manualSortOrder"));
            case ACCOUNT_STATUS -> Sort.unsorted();
            case RELEVANCE -> defaultRelevanceSort();
        };
    }

    /**
     * Релевантность: объём профиля, затем аватар, дата создания.
     * Ручной номер админа в этот порядок не входит.
     * Нельзя использовать .nullsLast() / .nullsFirst() с Specification — см. {@link #resolve(FilterStudentReq)}.
     * На PostgreSQL для ASC NULL по умолчанию в конце.
     */
    private static Sort defaultRelevanceSort() {
        return Sort.by(
                Sort.Order.desc("profileTextScore"),
                Sort.Order.asc("imagePath"),
                Sort.Order.desc("timestamps.createdAt"));
    }

    private static boolean isCatalogRank(FilterStudentReq filter) {
        if (filter == null) {
            return false;
        }
        if (filter.findString() != null && !filter.findString().isBlank()) {
            return false;
        }
        boolean useRanking = filter.useDefaultRanking() == null || filter.useDefaultRanking();
        StudentSortField field = filter.sortBy();
        return useRanking && (field == null || field == StudentSortField.RELEVANCE);
    }

    private static Sort.Direction toSpringDirection(StudentSortDirection d) {
        if (d == null) {
            return Sort.Direction.DESC;
        }
        return d == StudentSortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
    }
}
