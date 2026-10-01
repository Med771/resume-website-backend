package ru.ai.sin.logic.user;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import ru.ai.sin.logic.user.dto.FilterUserReq;
import ru.ai.sin.models.enums.RoleEnum;

import java.util.ArrayList;
import java.util.List;

public final class UserSpecifications {

    private UserSpecifications() {}

    public static Specification<UserEnt> byFilters(FilterUserReq filterUserReq) {
        return (root, query, cb) -> {
            if (query == null) {
                return null;
            }

            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("role"), RoleEnum.RECRUITER));

            if (filterUserReq != null && filterUserReq.username() != null && !filterUserReq.username().isBlank()) {
                String pattern = "%" + filterUserReq.username().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("username")), pattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
