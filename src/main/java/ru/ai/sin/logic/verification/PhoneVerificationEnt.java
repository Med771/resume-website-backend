package ru.ai.sin.logic.verification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import ru.ai.sin.models.enums.PhoneVerificationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "phone_verifications")
@Getter
@Setter
@NoArgsConstructor
public class PhoneVerificationEnt {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(name = "phone_number", nullable = false, length = 32)
    private String phoneNumber;

    @Column(name = "telegram_user_id", length = 32)
    private String telegramUserId;

    @Column(name = "telegram_chat_id")
    private Long telegramChatId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PhoneVerificationStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "otp_code_hash", length = 255)
    private String otpCodeHash;
}
