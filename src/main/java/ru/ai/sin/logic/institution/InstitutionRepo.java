package ru.ai.sin.logic.institution;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Set;
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

    boolean existsByStudent_Id(UUID studentId);

    @Query("select distinct i.student.id from InstitutionEnt i where i.student.id in :ids")
    Set<UUID> findStudentIdsByStudentIdIn(@Param("ids") Collection<UUID> ids);
}
