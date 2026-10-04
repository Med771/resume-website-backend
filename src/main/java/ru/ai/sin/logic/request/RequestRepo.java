package ru.ai.sin.logic.request;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import ru.ai.sin.models.enums.ResultEnum;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface RequestRepo extends JpaRepository<RequestEnt, Long>, JpaSpecificationExecutor<RequestEnt> {

    @EntityGraph(attributePaths = {"recruiter", "student", "student.speciality", "appChat"})
    RequestEnt findById(long id);

    @NonNull
    @EntityGraph(attributePaths = {"recruiter", "student", "student.speciality", "appChat"})
    Page<RequestEnt> findAll(Specification<RequestEnt> spec,
                             @NonNull Pageable pageable);

    @NonNull
    @EntityGraph(attributePaths = {"recruiter", "student", "student.speciality", "appChat"})
    List<RequestEnt> findAll(Specification<RequestEnt> spec);

    boolean existsByRecruiter_IdAndStudent_IdAndResultIn(
            UUID recruiterId,
            UUID studentId,
            Collection<ResultEnum> results);

    void deleteByStudent_Id(UUID studentId);

    void deleteByRecruiter_Id(UUID recruiterId);

    List<RequestEnt> findByAppChat_IdOrderByIdDesc(UUID appChatId);

    void deleteByAppChat_Id(UUID appChatId);
}
