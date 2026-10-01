package ru.ai.sin.logic.company;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.ai.sin.logic.company.dto.FilterCompanyReq;

import java.util.ArrayList;
import java.util.List;

public class CompanySpecifications {

    private CompanySpecifications() {}

    public static Specification<CompanyEnt> byFilters(FilterCompanyReq filterCompanyReq) {
        return (root, query, cb) -> {

            if (query == null || filterCompanyReq == null) {
                return null;
            }

            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            if (filterCompanyReq.name() != null) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("name")),
                                "%" + filterCompanyReq.name().toLowerCase() + "%"
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
