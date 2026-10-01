package ru.ai.sin.logic.recruiter;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import ru.ai.sin.logic.recruiter.dto.FilterRecruiterReq;

import java.util.ArrayList;
import java.util.List;

public final class RecruiterSpecifications {

    private RecruiterSpecifications() {}

    public static Specification<RecruiterEnt> byFilters(FilterRecruiterReq filterRecruiterReq) {
        return (root, query, cb) -> {

            if (query == null || filterRecruiterReq == null) {
                return null;
            }

            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            if (filterRecruiterReq.name() != null) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("name")),
                                "%" + filterRecruiterReq.name().toLowerCase() + "%"
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
