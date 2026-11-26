package ru.ai.sin.logic.recruitment;

import org.springframework.data.jpa.domain.Specification;
import ru.ai.sin.models.enums.RecruitmentRequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public final class RecruitmentRequestSpecifications {

    private RecruitmentRequestSpecifications() {
    }

    public static Specification<RecruitmentRequestEnt> forAdmin(RecruitmentRequestStatus status) {
        return (root, query, cb) -> status == null
                ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<RecruitmentRequestEnt> forRecruiter(UUID recruiterId, RecruitmentRequestStatus status) {
        return (root, query, cb) -> {
            var own = cb.equal(root.get("recruiterId"), recruiterId);
            if (status == null) {
                return own;
            }
            return cb.and(own, cb.equal(root.get("status"), status));
        };
    }

    public static Specification<RecruitmentRequestEnt> openInWindow(LocalDateTime now) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), RecruitmentRequestStatus.OPEN),
                cb.or(cb.isNull(root.get("publishedFrom")), cb.lessThanOrEqualTo(root.get("publishedFrom"), now)),
                cb.or(cb.isNull(root.get("publishedTo")), cb.greaterThanOrEqualTo(root.get("publishedTo"), now))
        );
    }
}
