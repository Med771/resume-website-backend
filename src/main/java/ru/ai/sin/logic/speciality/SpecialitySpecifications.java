package ru.ai.sin.logic.speciality;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import ru.ai.sin.logic.speciality.dto.FilterSpecialityReq;

import java.util.ArrayList;
import java.util.List;

public class SpecialitySpecifications {

    private SpecialitySpecifications() {}

    public static Specification<SpecialityEnt> byFilters(FilterSpecialityReq filterSpecialityReq) {
        return (root, query, cb) -> {

            if (query == null || filterSpecialityReq == null) {
                return null;
            }

            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            if (filterSpecialityReq.name() != null) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("name")),
                                "%" + filterSpecialityReq.name().toLowerCase() + "%"
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
