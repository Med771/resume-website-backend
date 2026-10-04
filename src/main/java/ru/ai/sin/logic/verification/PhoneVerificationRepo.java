package ru.ai.sin.logic.verification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ai.sin.models.enums.PhoneVerificationStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface PhoneVerificationRepo extends JpaRepository<PhoneVerificationEnt, UUID> {

    List<PhoneVerificationEnt> findByTelegramChatIdAndStatusOrderByCreatedAtDesc(
            Long telegramChatId, PhoneVerificationStatus status);

    List<PhoneVerificationEnt> findByPhoneNumberAndStatusOrderByCreatedAtDesc(
            String phoneNumber, PhoneVerificationStatus status);
}
