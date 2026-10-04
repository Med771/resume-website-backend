package ru.ai.sin.logic.vacancy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.skill.SkillMapper;
import ru.ai.sin.logic.skill.SkillOrder;
import ru.ai.sin.logic.skill.dto.SkillDTO;
import ru.ai.sin.logic.vacancy.dto.FilterVacancyModerationReq;
import ru.ai.sin.logic.vacancy.dto.PatchVacancyVitrinaReq;
import ru.ai.sin.logic.vacancy.dto.ReorderVacanciesReq;
import ru.ai.sin.logic.vacancy.dto.VacancyDTO;
import ru.ai.sin.logic.vacancy.dto.VacancyModerationRejectReq;
import ru.ai.sin.models.PageResponse;
import ru.ai.sin.models.enums.VacancyStatus;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    public PageResponse<VacancyDTO> filter(Pageable pageable, FilterVacancyModerationReq filter) {
        Page<VacancyEnt> page = vacancyRepo.findAll(
                VacancySpecifications.moderationFilters(filter),
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

    @Override
    @Transactional
    public void reorder(ReorderVacanciesReq req) {
        List<UUID> ids = req.orderedIds();
        Set<UUID> unique = new HashSet<>(ids);
        if (unique.size() != ids.size()) {
            throw new BadRequestException("Duplicate ids in reorder list");
        }
        for (int i = 0; i < ids.size(); i++) {
            UUID id = ids.get(i);
            VacancyEnt v = vacancyRepo.findById(id)
                    .orElseThrow(() -> new NotFoundException("Вакансия не найдена: " + id));
            v.setManualSortOrder(i);
            vacancyRepo.save(v);
        }
    }

    @Override
    @Transactional
    public VacancyDTO patchVitrina(UUID id, PatchVacancyVitrinaReq req) {
        VacancyEnt v = vacancyRepo.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Вакансия не найдена: " + id));
        if (req.visibleToAnonymous() != null) {
            v.setVisibleToAnonymous(req.visibleToAnonymous());
        }
        if (req.manualSortOrder() != null) {
            v.setManualSortOrder(req.manualSortOrder());
        }
        vacancyRepo.save(v);
        return toDto(v);
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
