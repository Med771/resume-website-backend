package ru.ai.sin.logic.city;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.ai.sin.logic.city.dto.FilterCityReq;

import java.util.ArrayList;
import java.util.List;

public class CitySpecifications {

    private CitySpecifications() {
    }

    public static Specification<CityEnt> byFilters(FilterCityReq filter) {
        return (root, query, cb) -> {
            if (query == null) {
                return null;
            }
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();
            if (filter != null && filter.name() != null && !filter.name().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("name")),
                        "%" + filter.name().trim().toLowerCase() + "%"
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
