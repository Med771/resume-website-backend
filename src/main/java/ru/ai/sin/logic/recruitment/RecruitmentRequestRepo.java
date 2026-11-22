package ru.ai.sin.logic.recruitment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface RecruitmentRequestRepo extends JpaRepository<RecruitmentRequestEnt, UUID>, JpaSpecificationExecutor<RecruitmentRequestEnt> {
}
