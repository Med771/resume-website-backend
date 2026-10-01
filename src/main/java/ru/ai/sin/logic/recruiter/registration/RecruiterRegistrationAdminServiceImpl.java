package ru.ai.sin.logic.recruiter.registration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.recruiter.RecruiterMapper;
import ru.ai.sin.logic.recruiter.RecruiterRepo;
import ru.ai.sin.logic.recruiter.dto.AddRecruiterReq;
import ru.ai.sin.logic.recruiter.registration.dto.FilterRecruiterRegistrationReq;
import ru.ai.sin.logic.recruiter.registration.dto.RecruiterRegistrationApproveResultDTO;
import ru.ai.sin.logic.recruiter.registration.dto.RecruiterRegistrationRejectReq;
import ru.ai.sin.logic.recruiter.registration.dto.RecruiterRegistrationRequestDTO;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.models.PageResponse;
import ru.ai.sin.models.enums.RecruiterRegistrationStatus;
import ru.ai.sin.models.enums.RoleEnum;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecruiterRegistrationAdminServiceImpl implements RecruiterRegistrationAdminService {

    private final RecruiterRegistrationRequestRepo registrationRequestRepo;
    private final UserRepo userRepo;
    private final RecruiterRepo recruiterRepo;
    private final RecruiterMapper recruiterMapper;
    private final SecurityHelper securityHelper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RecruiterRegistrationRequestDTO> filter(Pageable pageable, FilterRecruiterRegistrationReq filter) {
        Page<RecruiterRegistrationRequestEnt> page = registrationRequestRepo.findAll(
                RecruiterRegistrationSpecifications.byFilters(filter == null
                        ? new FilterRecruiterRegistrationReq(null, null)
                        : filter),
                pageable
        );
        return new PageResponse<>(
                page.getContent().stream().map(this::toDto).toList(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    @Transactional
    public RecruiterRegistrationApproveResultDTO approve(UUID id) {
        RecruiterRegistrationRequestEnt r = registrationRequestRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Заявка не найдена: " + id));
        if (r.getStatus() != RecruiterRegistrationStatus.PENDING) {
            throw new BadRequestException("Заявка уже обработана");
        }
        if (userRepo.existsByUsername(r.getUsername())) {
            throw new BadRequestException("Пользователь с таким логином уже существует — отклоните заявку");
        }
        if (r.getEmail() != null && !r.getEmail().isBlank()
                && recruiterRepo.existsByNormalizedEmail(r.getEmail())) {
            throw new BadRequestException("Профиль рекрутера с таким email уже есть — отклоните заявку");
        }

        AddRecruiterReq addRecruiterReq = new AddRecruiterReq(
                r.getCompanyName(),
                r.getFirstName(),
                r.getLastName(),
                r.getEmail(),
                r.getPhoneNumber(),
                r.getTelegramUsername()
        );
        RecruiterEnt recruiter = recruiterMapper.toEntity(addRecruiterReq);
        try {
            recruiter = recruiterRepo.save(recruiter);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Approve recruiter registration: integrity {}", ex.getMessage());
            throw new BadRequestException("Не удалось создать профиль рекрутера (возможно, дубликат email).");
        }

        UserEnt user = new UserEnt(
                RoleEnum.RECRUITER,
                r.getName(),
                r.getUsername(),
                r.getPasswordHash()
        );
        user.setRecruiter(recruiter);
        try {
            user = userRepo.save(user);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Approve recruiter registration: user integrity {}", ex.getMessage());
            throw new BadRequestException("Не удалось создать пользователя (логин занят).");
        }

        r.setStatus(RecruiterRegistrationStatus.APPROVED);
        r.setProcessedAt(LocalDateTime.now());
        r.setProcessedByUsername(securityHelper.getCurrentUsername());
        r.setApprovedUserId(user.getId());
        registrationRequestRepo.save(r);

        log.info("Recruiter registration approved: requestId={} userId={} recruiterId={}", id, user.getId(), recruiter.getId());
        return new RecruiterRegistrationApproveResultDTO(user.getId(), recruiter.getId());
    }

    @Override
    @Transactional
    public void reject(UUID id, RecruiterRegistrationRejectReq body) {
        RecruiterRegistrationRequestEnt r = registrationRequestRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Заявка не найдена: " + id));
        if (r.getStatus() != RecruiterRegistrationStatus.PENDING) {
            throw new BadRequestException("Заявка уже обработана");
        }
        String reason = body != null && body.reason() != null && !body.reason().isBlank()
                ? body.reason().trim()
                : null;
        r.setStatus(RecruiterRegistrationStatus.REJECTED);
        r.setRejectReason(reason);
        r.setProcessedAt(LocalDateTime.now());
        r.setProcessedByUsername(securityHelper.getCurrentUsername());
        registrationRequestRepo.save(r);
        log.info("Recruiter registration rejected: requestId={} by={}", id, r.getProcessedByUsername());
    }

    private RecruiterRegistrationRequestDTO toDto(RecruiterRegistrationRequestEnt e) {
        var ts = e.getTimestamps();
        return new RecruiterRegistrationRequestDTO(
                e.getId(),
                e.getUsername(),
                e.getName(),
                e.getCompanyName(),
                e.getCity(),
                e.getFirstName(),
                e.getLastName(),
                e.getMiddleName(),
                e.getEmail(),
                e.getPhoneNumber(),
                e.getTelegramUsername(),
                e.isMarketingConsent(),
                e.getStatus(),
                e.getRejectReason(),
                e.getProcessedAt(),
                e.getProcessedByUsername(),
                e.getApprovedUserId(),
                ts != null ? ts.getCreatedAt() : null
        );
    }
}
