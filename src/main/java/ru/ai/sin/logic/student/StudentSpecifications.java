package ru.ai.sin.logic.student;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import ru.ai.sin.logic.student.dto.FilterStudentReq;
import ru.ai.sin.logic.student.dto.StudentSortDirection;
import ru.ai.sin.logic.student.dto.StudentSortField;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.models.enums.AccountStatus;

import ru.ai.sin.logic.skill.SkillEnt;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class StudentSpecifications {

    public enum FindText {
        NAME,
        BIO
    }

    private StudentSpecifications() {}

    /**
     * Сначала подстрока в ФИО. Если никого нет, тот же фильтр по описанию.
     * Пустая страница при ненулевом результате по имени описание не открывает.
     */
    public static Page<StudentEnt> findPage(
            StudentRepo studentRepo,
            Pageable pageable,
            FilterStudentReq filterStudentReq,
            boolean includeHiddenFromCatalog,
            boolean requirePublicConsent,
            boolean requireApprovedAccount,
            UUID alwaysIncludeStudentId
    ) {
        Page<StudentEnt> byName = studentRepo.findAll(
                byFilters(filterStudentReq, includeHiddenFromCatalog, requirePublicConsent,
                        requireApprovedAccount, alwaysIncludeStudentId, FindText.NAME),
                pageable);
        if (!hasFindString(filterStudentReq) || byName.getTotalElements() > 0) {
            return byName;
        }
        return studentRepo.findAll(
                byFilters(filterStudentReq, includeHiddenFromCatalog, requirePublicConsent,
                        requireApprovedAccount, alwaysIncludeStudentId, FindText.BIO),
                pageable);
    }

    private static boolean hasFindString(FilterStudentReq filterStudentReq) {
        return filterStudentReq != null
                && filterStudentReq.findString() != null
                && !filterStudentReq.findString().isBlank();
    }

    /**
     * @param includeHiddenFromCatalog если false, студенты с catalog_visible = false исключаются из выборки
     * @param requirePublicConsent если true, только студенты с public_profile_consent = true
     */
    public static Specification<StudentEnt> byFilters(
            FilterStudentReq filterStudentReq,
            boolean includeHiddenFromCatalog,
            boolean requirePublicConsent
    ) {
        return byFilters(filterStudentReq, includeHiddenFromCatalog, requirePublicConsent, false, null, FindText.NAME);
    }

    /**
     * @param requireApprovedAccount если true, в выборке только студенты с аккаунтом {@code APPROVED}
     * @param alwaysIncludeStudentId карточка этого id остаётся в выборке даже если она скрыта или аккаунт не одобрен
     */
    public static Specification<StudentEnt> byFilters(
            FilterStudentReq filterStudentReq,
            boolean includeHiddenFromCatalog,
            boolean requirePublicConsent,
            boolean requireApprovedAccount,
            UUID alwaysIncludeStudentId
    ) {
        return byFilters(filterStudentReq, includeHiddenFromCatalog, requirePublicConsent,
                requireApprovedAccount, alwaysIncludeStudentId, FindText.NAME);
    }

    public static Specification<StudentEnt> byFilters(
            FilterStudentReq filterStudentReq,
            boolean includeHiddenFromCatalog,
            boolean requirePublicConsent,
            boolean requireApprovedAccount,
            UUID alwaysIncludeStudentId,
            FindText findText
    ) {

        return (root, query, cb) -> {

            if (query == null || filterStudentReq == null) {
                return null;
            }

            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            if (requirePublicConsent) {
                predicates.add(cb.isTrue(root.get("publicProfileConsent")));
            }

            addAudiencePredicate(
                    predicates, root, query, cb,
                    includeHiddenFromCatalog, requireApprovedAccount, alwaysIncludeStudentId);

            if (hasFindString(filterStudentReq)) {
                String filter = "%" + filterStudentReq.findString().trim().toLowerCase() + "%";
                if (findText == FindText.BIO) {
                    predicates.add(cb.like(cb.lower(cb.coalesce(root.get("bio"), "")), filter));
                } else {
                    Expression<String> fullName = cb.lower(cb.concat(
                            cb.concat(
                                    cb.concat(cb.coalesce(root.get("userInformation").get("firstName"), ""), " "),
                                    cb.coalesce(root.get("userInformation").get("lastName"), "")),
                            cb.concat(" ", cb.coalesce(root.get("middleName"), ""))
                    ));
                    predicates.add(cb.like(fullName, filter));
                }
            }

            if (filterStudentReq.course() != null && !filterStudentReq.course().isEmpty()) {
                predicates.add(root.get("course").in(filterStudentReq.course()));
            }

            if (filterStudentReq.busyness() != null && !filterStudentReq.busyness().isEmpty()) {
                predicates.add(root.get("busyness").in(filterStudentReq.busyness()));
            }

            if (filterStudentReq.bornBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("birthDate"), filterStudentReq.bornBefore()));
            }

            if (filterStudentReq.bornAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("birthDate"), filterStudentReq.bornAfter()));
            }

            if (filterStudentReq.specialitiesIds() != null && !filterStudentReq.specialitiesIds().isEmpty()) {
                predicates.add(root.get("speciality").get("id").in(filterStudentReq.specialitiesIds()));
            }

            if (filterStudentReq.skillsIds() != null && !filterStudentReq.skillsIds().isEmpty()) {
                if (filterStudentReq.sortBy() == StudentSortField.ACCOUNT_STATUS) {
                    predicates.add(hasAnySkill(root, query, cb, filterStudentReq.skillsIds()));
                } else {
                    Join<StudentEnt, SkillEnt> skillsJoin = root.join("skills", JoinType.INNER);
                    predicates.add(skillsJoin.get("id").in(filterStudentReq.skillsIds()));
                }
            }

            if (filterStudentReq.catalogVisible() != null) {
                predicates.add(cb.equal(root.get("catalogVisible"), filterStudentReq.catalogVisible()));
            }

            if (filterStudentReq.publicProfileConsent() != null) {
                predicates.add(cb.equal(root.get("publicProfileConsent"), filterStudentReq.publicProfileConsent()));
            }

            orderByAccountStatus(root, query, cb, filterStudentReq);

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addAudiencePredicate(
            List<Predicate> predicates,
            Root<StudentEnt> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            boolean includeHiddenFromCatalog,
            boolean requireApprovedAccount,
            UUID alwaysIncludeStudentId
    ) {
        List<Predicate> audience = new ArrayList<>();
        if (!includeHiddenFromCatalog) {
            audience.add(cb.isTrue(root.get("catalogVisible")));
        }
        if (requireApprovedAccount) {
            audience.add(approvedAccount(root, query, cb));
        }
        if (audience.isEmpty()) {
            return;
        }
        Predicate visible = cb.and(audience.toArray(new Predicate[0]));
        if (alwaysIncludeStudentId == null) {
            predicates.add(visible);
            return;
        }
        predicates.add(cb.or(visible, cb.equal(root.get("id"), alwaysIncludeStudentId)));
    }

    /**
     * Статус лежит на users. {@link StudentSortResolver} для этого поля отдаёт пустой Sort,
     * чтобы Spring Data не затёр orderBy спецификации.
     */
    private static void orderByAccountStatus(
            Root<StudentEnt> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            FilterStudentReq filter
    ) {
        if (filter.sortBy() != StudentSortField.ACCOUNT_STATUS || isCountQuery(query)) {
            return;
        }
        query.distinct(false);
        Expression<Integer> rank = accountStatusRank(root, query, cb);
        boolean ascending = filter.sortDirection() != StudentSortDirection.DESC;
        List<Order> orders = new ArrayList<>();
        orders.add(ascending ? cb.asc(rank) : cb.desc(rank));
        orders.add(cb.asc(root.get("id")));
        query.orderBy(orders);
    }

    private static boolean isCountQuery(CriteriaQuery<?> query) {
        Class<?> resultType = query.getResultType();
        return resultType == Long.class || resultType == long.class;
    }

    private static Expression<Integer> accountStatusRank(
            Root<StudentEnt> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb
    ) {
        Subquery<Integer> rank = query.subquery(Integer.class);
        Root<UserEnt> user = rank.from(UserEnt.class);
        Expression<Integer> statusRank = cb.<Integer>selectCase()
                .when(cb.equal(user.get("accountStatus"), AccountStatus.PENDING_APPROVAL), 0)
                .when(cb.equal(user.get("accountStatus"), AccountStatus.APPROVED), 1)
                .when(cb.equal(user.get("accountStatus"), AccountStatus.REJECTED), 2)
                .otherwise(3);
        rank.select(cb.min(statusRank));
        rank.where(cb.equal(user.get("student"), root));
        return rank;
    }

    private static Predicate hasAnySkill(
            Root<StudentEnt> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            List<Long> skillIds
    ) {
        Subquery<Integer> skills = query.subquery(Integer.class);
        Root<StudentEnt> student = skills.from(StudentEnt.class);
        Join<StudentEnt, SkillEnt> skill = student.join("skills");
        skills.select(cb.literal(1));
        skills.where(
                cb.equal(student.get("id"), root.get("id")),
                skill.get("id").in(skillIds));
        return cb.exists(skills);
    }

    private static Predicate approvedAccount(Root<StudentEnt> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<Integer> approved = query.subquery(Integer.class);
        Root<UserEnt> user = approved.from(UserEnt.class);
        approved.select(cb.literal(1));
        approved.where(
                cb.equal(user.get("student"), root),
                cb.equal(user.get("accountStatus"), AccountStatus.APPROVED));
        Subquery<Integer> published = query.subquery(Integer.class);
        Root<ru.ai.sin.logic.student.version.StudentProfileVersionEnt> version = published.from(
                ru.ai.sin.logic.student.version.StudentProfileVersionEnt.class);
        published.select(cb.literal(1));
        published.where(
                cb.equal(version.get("studentId"), root.get("id")),
                cb.equal(version.get("status"), ru.ai.sin.logic.student.version.StudentProfileVersionStatus.APPROVED));
        return cb.or(cb.exists(approved), cb.exists(published));
    }


}
