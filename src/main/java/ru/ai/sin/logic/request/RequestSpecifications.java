package ru.ai.sin.logic.request;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.ai.sin.logic.request.dto.FilterRequestReq;

import java.util.ArrayList;
import java.util.List;

public final class RequestSpecifications {

    private RequestSpecifications() {}

    public static Specification<RequestEnt> byFilters(
            FilterRequestReq filterRequestReq
    ) {
        return (root, query, cb) -> {

            if (query == null || filterRequestReq == null) {
                return null;
            }

            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            if (filterRequestReq.results() != null && !filterRequestReq.results().isEmpty()) {
                predicates.add(
                    root.get("result").in(filterRequestReq.results())
                );
            }

            if (filterRequestReq.recruiterId() != null) {
                predicates.add(
                    cb.equal(root.get("recruiter").get("id"), filterRequestReq.recruiterId())
                );
            }

            if (filterRequestReq.studentId() != null) {
                predicates.add(
                    cb.equal(root.get("student").get("id"), filterRequestReq.studentId())
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

