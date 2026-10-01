package ru.ai.sin.logic.portfolio;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import ru.ai.sin.logic.portfolio.dto.FilterPortfolioReq;

import java.util.ArrayList;
import java.util.List;

public final class PortfolioSpecifications {
    private PortfolioSpecifications() {}

    public static Specification<PortfolioEnt> byFilters(FilterPortfolioReq filterPortfolioReq) {
        return (root, query, cb) -> {

            if (query == null || filterPortfolioReq == null) {
                return null;
            }

            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            if (filterPortfolioReq.studentId() != null) {
                predicates.add(cb.equal(root.get("student").get("id"), filterPortfolioReq.studentId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
