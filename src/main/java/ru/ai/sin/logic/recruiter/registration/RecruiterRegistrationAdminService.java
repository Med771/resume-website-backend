package ru.ai.sin.logic.recruiter.registration;

import org.springframework.data.domain.Pageable;
import ru.ai.sin.logic.recruiter.registration.dto.FilterRecruiterRegistrationReq;
import ru.ai.sin.logic.recruiter.registration.dto.RecruiterRegistrationApproveResultDTO;
import ru.ai.sin.logic.recruiter.registration.dto.RecruiterRegistrationRejectReq;
import ru.ai.sin.logic.recruiter.registration.dto.RecruiterRegistrationRequestDTO;
import ru.ai.sin.models.PageResponse;

import java.util.UUID;

public interface RecruiterRegistrationAdminService {

    PageResponse<RecruiterRegistrationRequestDTO> filter(Pageable pageable, FilterRecruiterRegistrationReq filter);

    RecruiterRegistrationApproveResultDTO approve(UUID id);

    void reject(UUID id, RecruiterRegistrationRejectReq body);
}
