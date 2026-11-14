package ru.ai.sin.logic.vacancy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.skill.SkillMapper;
import ru.ai.sin.logic.skill.SkillOrder;
import ru.ai.sin.logic.skill.dto.SkillDTO;
import ru.ai.sin.logic.vacancy.dto.VacancyDTO;
import ru.ai.sin.logic.vacancy.dto.VacancyModerationRejectReq;
import ru.ai.sin.models.PageResponse;
import ru.ai.sin.models.enums.VacancyStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VacancyModerationAdminServiceImpl implements VacancyModerationAdminService {

    private final VacancyRepo vacancyRepo;
    private final SkillMapper skillMapper;
    private final SecurityHelper securityHelper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VacancyDTO> list(int page, int size) {
        Page<VacancyEnt> result = vacancyRepo.findAll(PageRequest.of(page, size));
        return new PageResponse<>(
                result.getContent().stream().map(this::toDto).toList(),
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public VacancyDTO getById(UUID id) {
        VacancyEnt v = vacancyRepo.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Вакансия не найдена: " + id));
        return toDto(v);
    }

    @Override
    @Transactional
    public VacancyDTO approve(UUID id) {
        VacancyEnt v = vacancyRepo.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Вакансия не найдена: " + id));
        if (v.getStatus() != VacancyStatus.PENDING_REVIEW) {
            throw new BadRequestException("Одобрить можно только вакансию на модерации");
        }
        v.setStatus(VacancyStatus.PUBLISHED);
        v.setModeratedAt(LocalDateTime.now());
        v.setModeratedByUsername(securityHelper.getCurrentUsername());
        v.setModerationRejectionReason(null);
        vacancyRepo.save(v);
        log.info("Vacancy approved: id={} by={}", id, v.getModeratedByUsername());
        return toDto(v);
    }

    @Override
    @Transactional
    public void reject(UUID id, VacancyModerationRejectReq body) {
        VacancyEnt v = vacancyRepo.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Вакансия не найдена: " + id));
        if (v.getStatus() != VacancyStatus.PENDING_REVIEW) {
            throw new BadRequestException("Отклонить можно только вакансию на модерации");
        }
        String reason = body != null && StringUtils.hasText(body.moderationRejectionReason())
                ? body.moderationRejectionReason().trim()
                : null;
        v.setStatus(VacancyStatus.REJECTED);
        v.setModerationRejectionReason(reason);
        v.setModeratedAt(LocalDateTime.now());
        v.setModeratedByUsername(securityHelper.getCurrentUsername());
        vacancyRepo.save(v);
        log.info("Vacancy rejected: id={} by={}", id, v.getModeratedByUsername());
    }

    private VacancyDTO toDto(VacancyEnt v) {
        List<SkillDTO> skills = v.getSkills().stream()
                .sorted(SkillOrder.byCreatedAtThenId())
                .map(skillMapper::toDTO)
                .toList();
        var ts = v.getTimestamps();
        return new VacancyDTO(
                v.getId(),
                v.getRecruiter().getId(),
                v.getTitle(),
                v.getDescription(),
                v.getCompanyName(),
                v.getCity(),
                v.getWorkFormat(),
                v.getEmploymentType(),
                v.getSpeciality() != null ? v.getSpeciality().getId() : null,
                v.getSpeciality() != null ? v.getSpeciality().getName() : null,
                skills,
                v.getStatus(),
                v.getPublishedFrom(),
                v.getPublishedTo(),
                v.getSlotsCount(),
                v.getSubmittedForReviewAt(),
                v.getModeratedAt(),
                v.getModeratedByUsername(),
                v.getModerationRejectionReason(),
                vacancyRepo.countApplicationsByVacancyId(v.getId()),
                null,
                ts != null ? ts.getCreatedAt() : null,
                v.getManualSortOrder(),
                v.isVisibleToAnonymous()
        );
    }
}
