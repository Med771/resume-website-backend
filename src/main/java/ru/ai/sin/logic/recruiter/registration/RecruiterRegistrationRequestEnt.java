package ru.ai.sin.logic.recruiter.registration;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.ai.sin.models.embeddables.TimeStamped;
import ru.ai.sin.models.enums.RecruiterRegistrationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "recruiter_registration_requests")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class RecruiterRegistrationRequestEnt {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 255)
    private String name;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "middle_name")
    private String middleName;

    @Column
    private String city;

    @Column(name = "marketing_consent", nullable = false)
    private boolean marketingConsent;

    @Column(name = "phone_verification_id")
    private UUID phoneVerificationId;

    @Column
    private String email;

    @Column(name = "phone_number", length = 32)
    private String phoneNumber;

    @Column(name = "telegram_username", length = 32)
    private String telegramUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RecruiterRegistrationStatus status;

    @Column(columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processed_by_username", length = 64)
    private String processedByUsername;

    @Column(name = "approved_user_id")
    private UUID approvedUserId;

    @Embedded
    private TimeStamped timestamps = new TimeStamped();
}
