package ru.ai.sin.logic.education;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import ru.ai.sin.logic.education.dto.FilterEducationReq;

import java.util.ArrayList;
import java.util.List;

public class EducationSpecifications {

    private EducationSpecifications() {}

    public static Specification<EducationEnt> byFilters(FilterEducationReq filterEducationReq) {
        return (root, query, cb) -> {

            if (query == null || filterEducationReq == null) {
                return null;
            }

            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            if (filterEducationReq.ids() != null && !filterEducationReq.ids().isEmpty()) {
                predicates.add(root.get("id").in(filterEducationReq.ids()));
            }

            if (filterEducationReq.institution() != null && !filterEducationReq.institution().isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("institution")),
                                "%" + filterEducationReq.institution().toLowerCase() + "%"
                        )
                );
            }

            if (filterEducationReq.additionalInfo() != null && !filterEducationReq.additionalInfo().isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("additionalInfo")),
                                "%" + filterEducationReq.additionalInfo().toLowerCase() + "%"
                        )
                );
            }

            if (filterEducationReq.webUrl() != null && !filterEducationReq.webUrl().isBlank()) {
                predicates.add(
                        cb.like(
                                root.get("webUrl"),
                                "%" + filterEducationReq.webUrl() + "%"
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
