package ru.ai.sin.logic.institution;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import ru.ai.sin.logic.institution.dto.FilterInstitutionReq;

import java.util.ArrayList;
import java.util.List;

public final class InstitutionSpecifications {

    private InstitutionSpecifications() {}

    public static Specification<InstitutionEnt> byFilters(
            FilterInstitutionReq filterInstitutionReq
    ) {
        return (root, query, cb) -> {

            if (query == null || filterInstitutionReq == null) {
                return null;
            }

            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            if (filterInstitutionReq.studentId() != null) {
                predicates.add(
                        cb.equal(root.get("student").get("id"), filterInstitutionReq.studentId())
                );
            }

            if (filterInstitutionReq.educationId() != null) {
                predicates.add(
                        cb.equal(root.get("education").get("id"), filterInstitutionReq.educationId())
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
