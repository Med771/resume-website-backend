package ru.ai.sin.logic.education;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface EducationRepo extends JpaRepository<EducationEnt, Long>, JpaSpecificationExecutor<EducationEnt> {
    Optional<EducationEnt> findFirstByInstitutionIgnoreCase(String institution);

    @NonNull
    Page<EducationEnt> findAll(Specification<EducationEnt> spec, @NonNull Pageable pageable);
}
