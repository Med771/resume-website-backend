package ru.ai.sin.logic.recruiter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecruiterRepo extends JpaRepository<RecruiterEnt, UUID>, JpaSpecificationExecutor<RecruiterEnt> {
    Optional<RecruiterEnt> findByContactInformationTelegramUserId(String telegramUserId);

    Optional<RecruiterEnt> findByUserInformationEmail(String email);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM RecruiterEnt r
            WHERE LOWER(TRIM(r.userInformation.email)) = LOWER(TRIM(:email))
            """)
    boolean existsByNormalizedEmail(@Param("email") String email);

    @NonNull
    Page<RecruiterEnt> findAll(Specification<RecruiterEnt> spec, @NonNull Pageable pageable);

    @Query("select count(r) from RecruiterEnt r")
    long countAllRecruiters();

    @Query("""
            select count(r) from RecruiterEnt r
            where r.timestamps.createdAt is not null
              and r.timestamps.createdAt >= :from
              and r.timestamps.createdAt < :to""")
    long countRecruitersCreatedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
