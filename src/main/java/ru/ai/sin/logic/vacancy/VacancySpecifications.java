package ru.ai.sin.logic.vacancy;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import ru.ai.sin.logic.skill.SkillEnt;
import ru.ai.sin.logic.vacancy.dto.FilterVacancyModerationReq;
import ru.ai.sin.logic.vacancy.dto.FilterVacancyReq;
import ru.ai.sin.models.enums.VacancyStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class VacancySpecifications {

    private VacancySpecifications() {}

    public static Specification<VacancyEnt> publishedFeed(FilterVacancyReq filter) {
        return (root, query, cb) -> {
            if (query == null) {
                return null;
            }
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            predicates.add(cb.equal(root.get("status"), VacancyStatus.PUBLISHED));
            predicates.add(cb.or(
                    cb.isNull(root.get("publishedFrom")),
                    cb.lessThanOrEqualTo(root.get("publishedFrom"), now)
            ));
            predicates.add(cb.or(
                    cb.isNull(root.get("publishedTo")),
                    cb.greaterThanOrEqualTo(root.get("publishedTo"), now)
            ));

            applyCommonFilters(filter, root, cb, predicates);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<VacancyEnt> anonymousVisible(FilterVacancyReq filter) {
        return (root, query, cb) -> {
            if (query == null) {
                return null;
            }
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            predicates.add(cb.equal(root.get("status"), VacancyStatus.PUBLISHED));
            predicates.add(cb.isTrue(root.get("visibleToAnonymous")));
            predicates.add(cb.or(
                    cb.isNull(root.get("publishedFrom")),
                    cb.lessThanOrEqualTo(root.get("publishedFrom"), now)
            ));
            predicates.add(cb.or(
                    cb.isNull(root.get("publishedTo")),
                    cb.greaterThanOrEqualTo(root.get("publishedTo"), now)
            ));

            applyCommonFilters(filter, root, cb, predicates);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<VacancyEnt> moderationFilters(FilterVacancyModerationReq filter) {
        return (root, query, cb) -> {
            if (query == null) {
                return null;
            }
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            VacancyStatus status = filter != null && filter.status() != null
                    ? filter.status()
                    : VacancyStatus.PENDING_REVIEW;
            predicates.add(cb.equal(root.get("status"), status));

            if (filter != null) {
                if (filter.recruiterId() != null) {
                    predicates.add(cb.equal(root.get("recruiter").get("id"), filter.recruiterId()));
                }
                if (StringUtils.hasText(filter.companyName())) {
                    String pattern = "%" + filter.companyName().trim().toLowerCase() + "%";
                    predicates.add(cb.like(cb.lower(root.get("companyName")), pattern));
                }
                if (StringUtils.hasText(filter.findString())) {
                    String pattern = "%" + filter.findString().trim().toLowerCase() + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("title")), pattern),
                            cb.like(cb.lower(root.get("description")), pattern)
                    ));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void applyCommonFilters(
            FilterVacancyReq filter,
            jakarta.persistence.criteria.Root<VacancyEnt> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            List<Predicate> predicates
    ) {
        if (filter == null) {
            return;
        }
        if (StringUtils.hasText(filter.findString())) {
            String pattern = "%" + filter.findString().trim().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.like(cb.lower(root.get("companyName")), pattern)
            ));
        }
        if (StringUtils.hasText(filter.city())) {
            predicates.add(cb.equal(cb.lower(root.get("city")), filter.city().trim().toLowerCase()));
        }
        if (filter.workFormats() != null && !filter.workFormats().isEmpty()) {
            predicates.add(root.get("workFormat").in(filter.workFormats()));
        }
        if (filter.employmentTypes() != null && !filter.employmentTypes().isEmpty()) {
            predicates.add(root.get("employmentType").in(filter.employmentTypes()));
        }
        if (filter.specialityIds() != null && !filter.specialityIds().isEmpty()) {
            predicates.add(root.get("speciality").get("id").in(filter.specialityIds()));
        }
        if (filter.skillIds() != null && !filter.skillIds().isEmpty()) {
            Join<VacancyEnt, SkillEnt> skillsJoin = root.join("skills", JoinType.INNER);
            predicates.add(skillsJoin.get("id").in(filter.skillIds()));
        }
    }
}
