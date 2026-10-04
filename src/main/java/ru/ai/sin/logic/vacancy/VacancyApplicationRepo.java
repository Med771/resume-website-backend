package ru.ai.sin.logic.vacancy;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ai.sin.models.enums.VacancyApplicationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VacancyApplicationRepo extends JpaRepository<VacancyApplicationEnt, UUID> {

    @EntityGraph(attributePaths = {"vacancy", "vacancy.recruiter", "student", "appChat"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<VacancyApplicationEnt> findWithDetailsById(UUID id);

    @EntityGraph(attributePaths = {"vacancy", "vacancy.recruiter", "student", "appChat"}, type = EntityGraph.EntityGraphType.LOAD)
    Page<VacancyApplicationEnt> findByVacancy_IdOrderByTimestamps_CreatedAtDesc(UUID vacancyId, Pageable pageable);

    @EntityGraph(attributePaths = {"vacancy", "vacancy.recruiter", "student", "appChat"}, type = EntityGraph.EntityGraphType.LOAD)
    Page<VacancyApplicationEnt> findByStudent_IdOrderByTimestamps_CreatedAtDesc(UUID studentId, Pageable pageable);

    Optional<VacancyApplicationEnt> findByVacancy_IdAndStudent_Id(UUID vacancyId, UUID studentId);

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
            FROM VacancyApplicationEnt a
            WHERE a.vacancy.recruiter.id = :recruiterId
              AND a.student.id = :studentId
              AND a.status = :status
            """)
    boolean existsByRecruiterAndStudentAndStatus(
            @Param("recruiterId") UUID recruiterId,
            @Param("studentId") UUID studentId,
            @Param("status") VacancyApplicationStatus status);

    @Modifying
    @Query("DELETE FROM VacancyApplicationEnt a WHERE a.vacancy.recruiter.id = :recruiterId")
    void deleteByVacancyRecruiterId(@Param("recruiterId") UUID recruiterId);

    List<VacancyApplicationEnt> findByAppChat_Id(UUID appChatId);

    @EntityGraph(attributePaths = {"vacancy", "vacancy.recruiter", "student", "appChat"}, type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT a FROM VacancyApplicationEnt a WHERE a.appChat.id = :chatId")
    List<VacancyApplicationEnt> findByAppChat_IdWithDetails(@Param("chatId") UUID chatId);

    void deleteByAppChat_Id(UUID appChatId);
}
