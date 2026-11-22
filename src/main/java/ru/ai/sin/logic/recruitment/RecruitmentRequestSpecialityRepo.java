package ru.ai.sin.logic.recruitment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RecruitmentRequestSpecialityRepo extends JpaRepository<RecruitmentRequestSpecialityEnt, RecruitmentRequestSpecialityId> {

    List<RecruitmentRequestSpecialityEnt> findByRecruitmentRequestId(UUID recruitmentRequestId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RecruitmentRequestSpecialityEnt e where e.recruitmentRequestId = :requestId")
    void deleteByRecruitmentRequestId(@Param("requestId") UUID requestId);
}
