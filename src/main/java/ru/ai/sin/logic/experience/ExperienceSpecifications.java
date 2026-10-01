package ru.ai.sin.logic.experience;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import ru.ai.sin.logic.experience.dto.FilterExperienceReq;

import java.util.ArrayList;
import java.util.List;

public final class ExperienceSpecifications {

    private ExperienceSpecifications() {}

    public static Specification<ExperienceEnt> byFilters(
            FilterExperienceReq filterExperienceReq
    ) {
        return (root, query, cb) -> {

            if (query == null || filterExperienceReq == null) {
                return null;
            }

            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            if (filterExperienceReq.studentId() != null) {
                predicates.add(
                    cb.equal(root.get("student").get("id"), filterExperienceReq.studentId())
                );
            }

            if (filterExperienceReq.companyId() != null) {
                predicates.add(
                    cb.equal(root.get("company").get("id"), filterExperienceReq.companyId())
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
