package ru.ai.sin.logic.recruiter.registration;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import ru.ai.sin.logic.recruiter.registration.dto.FilterRecruiterRegistrationReq;

import java.util.ArrayList;
import java.util.List;

public final class RecruiterRegistrationSpecifications {

    private RecruiterRegistrationSpecifications() {}

    public static Specification<RecruiterRegistrationRequestEnt> byFilters(FilterRecruiterRegistrationReq filter) {
        return (root, query, cb) -> {
            if (query == null || filter == null) {
                return null;
            }
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }

            if (StringUtils.hasText(filter.search())) {
                String pattern = "%" + filter.search().trim().toLowerCase() + "%";
                List<Predicate> ors = new ArrayList<>();
                ors.add(cb.like(cb.lower(root.get("username")), pattern));
                ors.add(cb.like(cb.lower(root.get("companyName")), pattern));
                ors.add(cb.and(cb.isNotNull(root.get("email")), cb.like(cb.lower(root.get("email")), pattern)));
                ors.add(cb.and(cb.isNotNull(root.get("firstName")), cb.like(cb.lower(root.get("firstName")), pattern)));
                ors.add(cb.and(cb.isNotNull(root.get("lastName")), cb.like(cb.lower(root.get("lastName")), pattern)));
                predicates.add(cb.or(ors.toArray(new Predicate[0])));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
