package ru.ai.sin.logic.account;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import ru.ai.sin.logic.account.dto.RecruiterApprovalSortField;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.student.dto.StudentSortDirection;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.RoleEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RecruiterApprovalSpecifications {

    private RecruiterApprovalSpecifications() {
    }

    public static Specification<UserEnt> recruiters(
            String findString,
            AccountStatus accountStatus,
            Boolean emailVerified,
            String city,
            RecruiterApprovalSortField sortBy,
            StudentSortDirection sortDirection
    ) {
        return (root, query, cb) -> {
            Join<UserEnt, RecruiterEnt> recruiter = root.join("recruiter", JoinType.INNER);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("role"), RoleEnum.RECRUITER));
            if (accountStatus != null) {
                predicates.add(cb.equal(root.get("accountStatus"), accountStatus));
            }
            if (emailVerified != null) {
                predicates.add(cb.equal(root.get("emailVerified"), emailVerified));
            }
            if (hasText(city)) {
                predicates.add(cb.like(cb.lower(recruiter.get("city")), "%" + city.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (hasText(findString)) {
                String pattern = "%" + findString.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(recruiter.get("companyName"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(recruiter.get("userInformation").get("lastName"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(recruiter.get("userInformation").get("firstName"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(recruiter.get("middleName"), "")), pattern)
                ));
            }
            if (!isCountQuery(query)) {
                applySort(root, recruiter, query, cb, sortBy, sortDirection);
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    static boolean matches(
            UserEnt user,
            String findString,
            AccountStatus accountStatus,
            Boolean emailVerified,
            String city
    ) {
        if (user == null || user.getRole() != RoleEnum.RECRUITER || user.getRecruiter() == null) {
            return false;
        }
        if (accountStatus != null && user.getAccountStatus() != accountStatus) {
            return false;
        }
        if (emailVerified != null && user.isEmailVerified() != emailVerified) {
            return false;
        }
        var recruiter = user.getRecruiter();
        if (hasText(city)) {
            String value = recruiter.getCity() == null ? "" : recruiter.getCity().toLowerCase(Locale.ROOT);
            if (!value.contains(city.trim().toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        if (hasText(findString)) {
            String needle = findString.trim().toLowerCase(Locale.ROOT);
            String company = recruiter.getCompanyName() == null ? "" : recruiter.getCompanyName().toLowerCase(Locale.ROOT);
            String last = recruiter.getUserInformation() == null || recruiter.getUserInformation().getLastName() == null
                    ? "" : recruiter.getUserInformation().getLastName().toLowerCase(Locale.ROOT);
            String first = recruiter.getUserInformation() == null || recruiter.getUserInformation().getFirstName() == null
                    ? "" : recruiter.getUserInformation().getFirstName().toLowerCase(Locale.ROOT);
            String middle = recruiter.getMiddleName() == null ? "" : recruiter.getMiddleName().toLowerCase(Locale.ROOT);
            if (!company.contains(needle) && !last.contains(needle) && !first.contains(needle) && !middle.contains(needle)) {
                return false;
            }
        }
        return true;
    }

    private static void applySort(
            Root<UserEnt> root,
            Join<UserEnt, RecruiterEnt> recruiter,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            RecruiterApprovalSortField sortBy,
            StudentSortDirection sortDirection
    ) {
        RecruiterApprovalSortField field = sortBy == null ? RecruiterApprovalSortField.CREATED_AT : sortBy;
        boolean ascending = field == RecruiterApprovalSortField.ACCOUNT_STATUS
                ? sortDirection != StudentSortDirection.DESC
                : sortDirection == StudentSortDirection.ASC;
        Expression<?> sortExpression = switch (field) {
            case COMPANY_NAME -> recruiter.get("companyName");
            case LAST_NAME -> recruiter.get("userInformation").get("lastName");
            case ACCOUNT_STATUS -> accountStatusRank(root, cb);
            case CREATED_AT -> recruiter.get("timestamps").get("createdAt");
        };
        query.orderBy(
                ascending ? cb.asc(sortExpression) : cb.desc(sortExpression),
                cb.asc(root.get("id"))
        );
    }

    private static Expression<Integer> accountStatusRank(Root<UserEnt> root, CriteriaBuilder cb) {
        return cb.<Integer>selectCase()
                .when(cb.equal(root.get("accountStatus"), AccountStatus.PENDING_APPROVAL), 0)
                .when(cb.equal(root.get("accountStatus"), AccountStatus.APPROVED), 1)
                .when(cb.equal(root.get("accountStatus"), AccountStatus.REJECTED), 2)
                .otherwise(3);
    }

    private static boolean isCountQuery(CriteriaQuery<?> query) {
        Class<?> resultType = query.getResultType();
        return resultType == Long.class || resultType == long.class;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
