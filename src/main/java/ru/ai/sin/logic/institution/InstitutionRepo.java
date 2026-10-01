package ru.ai.sin.logic.institution;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InstitutionRepo extends JpaRepository<InstitutionEnt, Long>, JpaSpecificationExecutor<InstitutionEnt> {

    @EntityGraph(attributePaths = {"education", "student"}, type = EntityGraph.EntityGraphType.LOAD)
    InstitutionEnt findWithEducationAndStudentById(Long id);

    @NonNull
    @EntityGraph(attributePaths = {"education", "student"}, type = EntityGraph.EntityGraphType.LOAD)
    Page<InstitutionEnt> findAll(
            Specification<InstitutionEnt> spec,
            @NonNull Pageable pageable
    );

    void deleteByStudent_Id(UUID studentId);

    List<InstitutionEnt> findAllByStudent_Id(UUID studentId);
}
