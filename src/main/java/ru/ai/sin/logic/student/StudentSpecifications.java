package ru.ai.sin.logic.student;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import ru.ai.sin.logic.student.dto.FilterStudentReq;

import ru.ai.sin.logic.skill.SkillEnt;

import java.util.ArrayList;
import java.util.List;

public final class StudentSpecifications {

    private StudentSpecifications() {}

    /**
     * @param includeHiddenFromCatalog если false, студенты с catalog_visible = false исключаются из выборки
     * @param requirePublicConsent если true, только студенты с public_profile_consent = true
     */
    public static Specification<StudentEnt> byFilters(
            FilterStudentReq filterStudentReq,
            boolean includeHiddenFromCatalog,
            boolean requirePublicConsent
    ) {

        return (root, query, cb) -> {

            if (query == null || filterStudentReq == null) {
                return null;
            }

            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            if (requirePublicConsent) {
                predicates.add(cb.isTrue(root.get("publicProfileConsent")));
            }

            if (!includeHiddenFromCatalog) {
                predicates.add(cb.isTrue(root.get("catalogVisible")));
            }

            if (filterStudentReq.findString() != null && !filterStudentReq.findString().isBlank()) {
                String filter = filterStudentReq.findString().toLowerCase();

                double threshold = 0.3;
                int len = filter.length();

                if (len <= 3) {threshold = 0.1;}
                else if (len <= 5) {threshold = 0.18;}
                else if (len <= 8) {threshold = 0.25;}

                Expression<String> fullName = cb.lower(cb.concat(
                        cb.concat(
                                cb.concat(
                                        root.get("userInformation").get("firstName"), " "),
                                root.get("userInformation").get("lastName")),
                        cb.concat(" ", root.get("middleName"))
                ));

                Expression<String> bio = cb.lower(root.get("bio"));

                Expression<Double> fullNameSimilarity = cb.function("word_similarity", Double.class, fullName, cb.literal(filter));

                Expression<Double> bioSimilarity = cb.function("similarity", Double.class, bio, cb.literal(filter));

                Predicate fullNameFuzzy = cb.greaterThanOrEqualTo(fullNameSimilarity, threshold);

                Predicate bioFuzzy = cb.greaterThanOrEqualTo(bioSimilarity, threshold);

                Predicate fullNameLike = cb.like(fullName, "%" + filter + "%");

                predicates.add(cb.or(fullNameFuzzy, fullNameLike, bioFuzzy));
            }

            if (filterStudentReq.course() != null && !filterStudentReq.course().isEmpty()) {
                predicates.add(root.get("course").in(filterStudentReq.course()));
            }

            if (filterStudentReq.busyness() != null && !filterStudentReq.busyness().isEmpty()) {
                predicates.add(root.get("busyness").in(filterStudentReq.busyness()));
            }

            if (filterStudentReq.bornBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("birthDate"), filterStudentReq.bornBefore()));
            }

            if (filterStudentReq.bornAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("birthDate"), filterStudentReq.bornAfter()));
            }

            if (filterStudentReq.specialitiesIds() != null && !filterStudentReq.specialitiesIds().isEmpty()) {
                predicates.add(root.get("speciality").get("id").in(filterStudentReq.specialitiesIds()));
            }

            if (filterStudentReq.skillsIds() != null && !filterStudentReq.skillsIds().isEmpty()) {
                Join<StudentEnt, SkillEnt> skillsJoin = root.join("skills", JoinType.INNER);

                predicates.add(skillsJoin.get("id").in(filterStudentReq.skillsIds()));
            }

            if (filterStudentReq.catalogVisible() != null) {
                predicates.add(cb.equal(root.get("catalogVisible"), filterStudentReq.catalogVisible()));
            }

            if (filterStudentReq.publicProfileConsent() != null) {
                predicates.add(cb.equal(root.get("publicProfileConsent"), filterStudentReq.publicProfileConsent()));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }


}
