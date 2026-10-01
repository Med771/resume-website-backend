package ru.ai.sin.logic.vacancy;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VacancyRepo extends JpaRepository<VacancyEnt, UUID>, JpaSpecificationExecutor<VacancyEnt> {

    @EntityGraph(attributePaths = {"recruiter", "speciality", "skills"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<VacancyEnt> findWithDetailsById(UUID id);

    List<VacancyEnt> findByRecruiter_IdOrderByTimestamps_CreatedAtDesc(UUID recruiterId);

    long countByRecruiter_Id(UUID recruiterId);

    @Query("SELECT COUNT(a) FROM VacancyApplicationEnt a WHERE a.vacancy.id = :vacancyId")
    long countApplicationsByVacancyId(@Param("vacancyId") UUID vacancyId);

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
            FROM VacancyApplicationEnt a
            WHERE a.vacancy.id = :vacancyId AND a.student.id = :studentId
            """)
    boolean existsApplicationByVacancyAndStudent(@Param("vacancyId") UUID vacancyId, @Param("studentId") UUID studentId);

    void deleteByRecruiter_Id(UUID recruiterId);
}
