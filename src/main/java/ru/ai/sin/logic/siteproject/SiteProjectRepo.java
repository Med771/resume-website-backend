package ru.ai.sin.logic.siteproject;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SiteProjectRepo extends JpaRepository<SiteProjectEnt, UUID> {

    /**
     * Только images через JOIN FETCH: нельзя грузить images и skills/students в одном EntityGraph —
     * Hibernate размножает элементы List (bag) из-за декартова произведения.
     */
    @Query("SELECT DISTINCT p FROM SiteProjectEnt p LEFT JOIN FETCH p.images ORDER BY p.sortOrder ASC")
    List<SiteProjectEnt> findAllWithImagesByOrderBySortOrderAsc();

    @Query("SELECT DISTINCT p FROM SiteProjectEnt p LEFT JOIN FETCH p.images ORDER BY p.sortOrder ASC")
    List<SiteProjectEnt> findAllWithDetailsByOrderBySortOrderAsc();

    @Query("SELECT DISTINCT p FROM SiteProjectEnt p LEFT JOIN FETCH p.images WHERE p.id = :id")
    Optional<SiteProjectEnt> findWithImagesById(@Param("id") UUID id);

    @Query("SELECT DISTINCT p FROM SiteProjectEnt p LEFT JOIN FETCH p.images WHERE p.id = :id")
    Optional<SiteProjectEnt> findWithDetailsById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"students"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<SiteProjectEnt> findWithStudentsById(UUID id);

    @Query("""
            SELECT s.id FROM SiteProjectEnt p JOIN p.students s WHERE p.id = :projectId
            ORDER BY s.userInformation.lastName, s.userInformation.firstName, s.id
            """)
    List<UUID> findStudentIdsByProjectId(@Param("projectId") UUID projectId);
}
