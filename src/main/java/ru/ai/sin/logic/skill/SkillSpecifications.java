package ru.ai.sin.logic.skill;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import ru.ai.sin.logic.skill.dto.FilterSkillReq;

import java.util.ArrayList;
import java.util.List;

public class SkillSpecifications {

    private SkillSpecifications() {}

    public static Specification<SkillEnt> byFilters(FilterSkillReq filterSkillReq) {
        return (root, query, cb) -> {

            if (query == null || filterSkillReq == null) {
                return null;
            }

            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            if (filterSkillReq.name() != null) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("name")),
                                "%" + filterSkillReq.name().toLowerCase() + "%"
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
