package ru.ai.sin.logic.student.version;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentProfileVersionRepo extends JpaRepository<StudentProfileVersionEnt, UUID> {

    Optional<StudentProfileVersionEnt> findFirstByStudentIdOrderByVersionNumberDesc(UUID studentId);

    List<StudentProfileVersionEnt> findByStudentIdOrderByVersionNumberAsc(UUID studentId);

    Optional<StudentProfileVersionEnt> findByStudentIdAndVersionNumber(UUID studentId, int versionNumber);

    boolean existsByStudentIdAndStatus(UUID studentId, StudentProfileVersionStatus status);

    List<StudentProfileVersionEnt> findByStatusAndGraceEndsAtBefore(
            StudentProfileVersionStatus status,
            LocalDateTime moment);

    void deleteByStudentIdAndStatus(UUID studentId, StudentProfileVersionStatus status);

    @Query(value = """
            SELECT student_id FROM student_profile_versions
            WHERE snapshot->>'imagePath' = :path
            ORDER BY version_number DESC
            LIMIT 1
            """, nativeQuery = true)
    UUID findStudentIdBySnapshotImagePath(@Param("path") String path);
}
