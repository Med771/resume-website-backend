package ru.ai.sin.logic.recruitment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RecruitmentRequestCityRepo extends JpaRepository<RecruitmentRequestCityEnt, RecruitmentRequestCityId> {

    List<RecruitmentRequestCityEnt> findByRecruitmentRequestId(UUID recruitmentRequestId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RecruitmentRequestCityEnt e where e.recruitmentRequestId = :requestId")
    void deleteByRecruitmentRequestId(@Param("requestId") UUID requestId);
}
