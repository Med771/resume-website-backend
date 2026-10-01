package ru.ai.sin.logic.recruiter.registration.dto;

import java.util.UUID;

public record RecruiterRegistrationApproveResultDTO(
        UUID approvedUserId,
        UUID recruiterId
) {
}
