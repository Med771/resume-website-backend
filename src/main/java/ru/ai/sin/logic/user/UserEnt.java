package ru.ai.sin.logic.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.convertor.AccountStatusConverter;
import ru.ai.sin.models.enums.convertor.RoleEnumConverter;
import ru.ai.sin.models.enums.RoleEnum;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserEnt {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(name = "role", length = 32)
    @Convert(converter = RoleEnumConverter.class)
    private RoleEnum role;

    private String name;

    @Column(length = 64, unique = true)
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,64}$", message = "Username must be 3-64 characters, letters, digits or _")
    private String username;

    @Column(name = "password_hash", length = 128)
    private String passwordHash;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "email_otp_hash", length = 128)
    private String emailOtpHash;

    @Column(name = "email_otp_expires_at")
    private LocalDateTime emailOtpExpiresAt;

    @Column(name = "account_status", length = 32, nullable = false)
    @Convert(converter = AccountStatusConverter.class)
    private AccountStatus accountStatus = AccountStatus.APPROVED;

    @Column(name = "hints_disabled", nullable = false)
    private boolean hintsDisabled = false;

    /** Телефон, подтверждённый при регистрации (для предзаполнения резюме) */
    @Column(name = "registration_phone", length = 32)
    private String registrationPhone;

    /** Email, указанный при регистрации (для предзаполнения резюме) */
    @Column(name = "registration_email", length = 255)
    private String registrationEmail;

    /** Профиль рекрутера для повторных заявок без повторного ввода данных */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id")
    private RecruiterEnt recruiter;

    /** Аккаунт студента (ЛК): один пользователь — один студент */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private StudentEnt student;

    public UserEnt(RoleEnum role, String username, String passwordHash) {
        this.role = role;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public UserEnt(RoleEnum role, String name, String username, String passwordHash) {
        this.role = role;
        this.name = name;
        this.username = username;
        this.passwordHash = passwordHash;
    }
}
