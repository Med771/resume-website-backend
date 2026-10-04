package ru.ai.sin.logic.experience;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ExperienceRepo extends JpaRepository<ExperienceEnt, Long>, JpaSpecificationExecutor<ExperienceEnt> {

    @EntityGraph(attributePaths = {"company", "student"}, type = EntityGraph.EntityGraphType.LOAD)
    ExperienceEnt findWithCompanyAndStudentById(Long id);

    List<ExperienceEnt> findAllByCompanyId(Long companyId);

    Set<ExperienceEnt> findAllByCompanyIdIn(Set<Long> companyIds);

    @NonNull
    @EntityGraph(attributePaths = {"student", "company"})
    Page<ExperienceEnt> findAll(Specification<ExperienceEnt> spec, @NonNull Pageable pageable);

    void deleteByStudent_Id(UUID studentId);

    List<ExperienceEnt> findAllByStudent_Id(UUID studentId);
}
