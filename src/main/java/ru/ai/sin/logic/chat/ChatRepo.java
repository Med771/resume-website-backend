package ru.ai.sin.logic.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRepo extends JpaRepository<ChatEnt, UUID> {

    @EntityGraph(attributePaths = {"recruiter", "student"})
    Page<ChatEnt> findAllByOrderByLastActivityAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"recruiter", "student"})
    Page<ChatEnt> findByRecruiter_IdOrderByLastActivityAtDesc(UUID recruiterId, Pageable pageable);

    @EntityGraph(attributePaths = {"recruiter", "student"})
    Page<ChatEnt> findByStudent_IdOrderByLastActivityAtDesc(UUID studentId, Pageable pageable);

    Optional<ChatEnt> findByRecruiter_IdAndStudent_Id(UUID recruiterId, UUID studentId);

    void deleteByStudent_Id(UUID studentId);

    void deleteByRecruiter_Id(UUID recruiterId);
}
