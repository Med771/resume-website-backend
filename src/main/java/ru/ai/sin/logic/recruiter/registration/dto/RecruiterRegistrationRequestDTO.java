package ru.ai.sin.logic.recruiter.registration.dto;

import ru.ai.sin.models.enums.RecruiterRegistrationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record RecruiterRegistrationRequestDTO(
        UUID id,
        String username,
        String name,
        String companyName,
        String city,
        String firstName,
        String lastName,
        String middleName,
        String email,
        String phoneNumber,
        String telegramUsername,
        boolean marketingConsent,
        RecruiterRegistrationStatus status,
        String rejectReason,
        LocalDateTime processedAt,
        String processedByUsername,
        UUID approvedUserId,
        LocalDateTime createdAt
) {
}
