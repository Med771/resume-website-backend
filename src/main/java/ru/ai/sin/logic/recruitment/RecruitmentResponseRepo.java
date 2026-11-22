package ru.ai.sin.logic.recruitment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecruitmentResponseRepo extends JpaRepository<RecruitmentResponseEnt, UUID> {

    boolean existsByRecruitmentRequestIdAndStudentId(UUID recruitmentRequestId, UUID studentId);

    @Query("select r from RecruitmentResponseEnt r where r.recruitmentRequestId = :requestId order by r.timestamps.createdAt asc")
    List<RecruitmentResponseEnt> findByRecruitmentRequestIdOrderByTimestampsCreatedAtAsc(@Param("requestId") UUID requestId);

    Optional<RecruitmentResponseEnt> findByIdAndRecruitmentRequestId(UUID id, UUID recruitmentRequestId);

    Optional<RecruitmentResponseEnt> findByRecruitmentRequestIdAndStudentId(UUID recruitmentRequestId, UUID studentId);
}
