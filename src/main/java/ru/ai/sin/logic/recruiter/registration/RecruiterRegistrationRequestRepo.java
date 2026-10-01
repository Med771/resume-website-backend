package ru.ai.sin.logic.recruiter.registration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.ai.sin.models.enums.RecruiterRegistrationStatus;

import java.util.UUID;

@Repository
public interface RecruiterRegistrationRequestRepo extends
        JpaRepository<RecruiterRegistrationRequestEnt, UUID>,
        JpaSpecificationExecutor<RecruiterRegistrationRequestEnt> {

    boolean existsByStatusAndUsernameIgnoreCase(RecruiterRegistrationStatus status, String username);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM RecruiterRegistrationRequestEnt r
            WHERE r.status = :status
              AND r.email IS NOT NULL
              AND TRIM(r.email) <> ''
              AND LOWER(TRIM(r.email)) = LOWER(TRIM(:email))
            """)
    boolean existsByStatusAndNormalizedEmail(
            @Param("status") RecruiterRegistrationStatus status,
            @Param("email") String email
    );
}
