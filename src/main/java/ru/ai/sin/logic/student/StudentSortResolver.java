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
            case BIRTH_DATE -> Sort.by(new Sort.Order(dir, "birthDate"));
            case PROFILE_TEXT_SCORE -> Sort.by(new Sort.Order(dir, "profileTextScore"));
            case MANUAL_SORT_ORDER -> Sort.by(new Sort.Order(dir, "manualSortOrder"));
            case RELEVANCE -> defaultRelevanceSort();
        };
    }

    /**
     * Релевантность: ручной порядок, объём профиля, затем аватар, дата создания.
     * Нельзя использовать .nullsLast() / .nullsFirst() с Specification — см. {@link #resolve(FilterStudentReq)}.
     * На PostgreSQL для ASC NULL по умолчанию в конце.
     */
    private static Sort defaultRelevanceSort() {
        return Sort.by(
                Sort.Order.asc("manualSortOrder"),
                Sort.Order.desc("profileTextScore"),
                Sort.Order.asc("imagePath"),
                Sort.Order.desc("timestamps.createdAt"));
    }

    private static Sort.Direction toSpringDirection(StudentSortDirection d) {
        if (d == null) {
            return Sort.Direction.DESC;
        }
        return d == StudentSortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
    }
}
