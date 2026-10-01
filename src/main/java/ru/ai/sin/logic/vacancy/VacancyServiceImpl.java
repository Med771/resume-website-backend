package ru.ai.sin.logic.vacancy;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.skill.SkillEnt;
import ru.ai.sin.logic.skill.SkillMapper;
import ru.ai.sin.logic.skill.SkillOrder;
import ru.ai.sin.logic.skill.SkillRepo;
import ru.ai.sin.logic.skill.dto.SkillDTO;
import ru.ai.sin.logic.speciality.SpecialityEnt;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.vacancy.dto.CreateVacancyReq;
import ru.ai.sin.logic.vacancy.dto.FilterVacancyReq;
import ru.ai.sin.logic.vacancy.dto.UpdateVacancyReq;
import ru.ai.sin.logic.vacancy.dto.VacancyCardDTO;
import ru.ai.sin.logic.vacancy.dto.VacancyDTO;
import ru.ai.sin.models.PageResponse;
import ru.ai.sin.models.enums.RoleEnum;
import ru.ai.sin.models.enums.VacancyStatus;
import ru.ai.sin.tools.SpecialityTools;
import ru.ai.sin.tools.UserTools;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VacancyServiceImpl implements VacancyService {

    private static final int MIN_DESCRIPTION_LENGTH = 20;

    private final VacancyRepo vacancyRepo;
    private final SkillRepo skillRepo;
    private final SkillMapper skillMapper;
    private final UserTools userTools;
    private final SecurityHelper securityHelper;
    private final SpecialityTools specialityTools;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VacancyCardDTO> listPublishedFeed(Pageable pageable, FilterVacancyReq filter) {
        Page<VacancyEnt> page = vacancyRepo.findAll(
                VacancySpecifications.publishedFeed(filter),
                pageable
        );
        UUID studentId = currentStudentIdOrNull();
        return new PageResponse<>(
                page.getContent().stream().map(v -> toCardDto(v, studentId)).toList(),
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
        if (!canViewVacancy(v)) {
            throw new NotFoundException("Вакансия не найдена: " + id);
        }
        return toDto(v, currentStudentIdOrNull());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VacancyDTO> listMine() {
        RecruiterEnt recruiter = requireCurrentRecruiter();
        return vacancyRepo.findByRecruiter_IdOrderByTimestamps_CreatedAtDesc(recruiter.getId()).stream()
                .map(v -> toDto(loadDetails(v.getId()), null))
                .toList();
    }

    @Override
    @Transactional
    public VacancyDTO create(CreateVacancyReq req) {
        RecruiterEnt recruiter = requireCurrentRecruiter();
        VacancyEnt v = new VacancyEnt();
        v.setRecruiter(recruiter);
        v.setCompanyName(recruiter.getCompanyName());
        v.setStatus(VacancyStatus.DRAFT);
        applyFields(v, req.title(), req.description(), req.city(), req.workFormat(), req.employmentType(),
                req.specialityId(), req.skillIds(), req.publishedFrom(), req.publishedTo(), req.slotsCount(),
                req.visibleToAnonymous());
        return toDto(vacancyRepo.save(v), null);
    }

    @Override
    @Transactional
    public VacancyDTO update(UUID id, UpdateVacancyReq req) {
        VacancyEnt v = loadOwnedEditable(id);
        applyFields(v, req.title(), req.description(), req.city(), req.workFormat(), req.employmentType(),
                req.specialityId(), req.skillIds(), req.publishedFrom(), req.publishedTo(), req.slotsCount(),
                req.visibleToAnonymous());
        return toDto(vacancyRepo.save(v), null);
    }

    @Override
    @Transactional
    public VacancyDTO submitForReview(UUID id) {
        VacancyEnt v = loadOwnedEditable(id);
        validateReadyForReview(v);
        v.setStatus(VacancyStatus.PENDING_REVIEW);
        v.setSubmittedForReviewAt(LocalDateTime.now());
        v.setModeratedAt(null);
        v.setModeratedByUsername(null);
        v.setModerationRejectionReason(null);
        return toDto(vacancyRepo.save(v), null);
    }

    @Override
    @Transactional
    public VacancyDTO close(UUID id) {
        VacancyEnt v = loadOwned(id);
        if (v.getStatus() != VacancyStatus.PUBLISHED) {
            throw new BadRequestException("Закрыть можно только опубликованную вакансию");
        }
        v.setStatus(VacancyStatus.CLOSED);
        return toDto(vacancyRepo.save(v), null);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        VacancyEnt v = loadOwnedOrAdmin(id);
        if (v.getStatus() == VacancyStatus.DRAFT
                && vacancyRepo.countApplicationsByVacancyId(id) == 0) {
            vacancyRepo.delete(v);
            return;
        }
        v.setStatus(VacancyStatus.ARCHIVED);
        vacancyRepo.save(v);
    }

    private VacancyEnt loadOwned(UUID id) {
        VacancyEnt v = vacancyRepo.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Вакансия не найдена: " + id));
        assertOwner(v);
        return v;
    }

    private VacancyEnt loadOwnedEditable(UUID id) {
        VacancyEnt v = loadOwned(id);
        if (v.getStatus() != VacancyStatus.DRAFT && v.getStatus() != VacancyStatus.REJECTED) {
            throw new BadRequestException("Редактировать можно только черновик или отклонённую вакансию");
        }
        return v;
    }

    private VacancyEnt loadOwnedOrAdmin(UUID id) {
        VacancyEnt v = vacancyRepo.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Вакансия не найдена: " + id));
        if (securityHelper.isCurrentUserAdmin()) {
            return v;
        }
        assertOwner(v);
        return v;
    }

    private VacancyEnt loadDetails(UUID id) {
        return vacancyRepo.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Вакансия не найдена: " + id));
    }

    private void assertOwner(VacancyEnt v) {
        RecruiterEnt recruiter = requireCurrentRecruiter();
        if (!v.getRecruiter().getId().equals(recruiter.getId())) {
            throw new AccessDeniedException("Нет доступа к этой вакансии");
        }
    }

    private RecruiterEnt requireCurrentRecruiter() {
        UserEnt user = userTools.findCurrentUserFetchingRecruiter()
                .orElseThrow(() -> new AccessDeniedException("Требуется авторизация"));
        if (user.getRecruiter() == null) {
            throw new BadRequestException("Сначала оформите профиль рекрутёра (GET /recruiter/me)");
        }
        return user.getRecruiter();
    }

    private boolean canViewVacancy(VacancyEnt v) {
        if (v.getStatus() == VacancyStatus.PUBLISHED && isInPublicationWindow(v)) {
            return true;
        }
        if (securityHelper.isCurrentUserAdmin()) {
            return true;
        }
        UserEnt user = userTools.findCurrentUserFetchingRecruiter().orElse(null);
        return user != null && user.getRecruiter() != null
                && user.getRecruiter().getId().equals(v.getRecruiter().getId());
    }

    public static boolean isInPublicationWindow(VacancyEnt v) {
        LocalDateTime now = LocalDateTime.now();
        if (v.getPublishedFrom() != null && v.getPublishedFrom().isAfter(now)) {
            return false;
        }
        return v.getPublishedTo() == null || !v.getPublishedTo().isBefore(now);
    }

    private UUID currentStudentIdOrNull() {
        return userTools.findCurrentUserFetchingLinks()
                .filter(u -> u.getRole() == RoleEnum.STUDENT && u.getStudent() != null)
                .map(u -> u.getStudent().getId())
                .orElse(null);
    }

    private void applyFields(
            VacancyEnt v,
            String title,
            String description,
            String city,
            ru.ai.sin.models.enums.WorkFormatEnum workFormat,
            ru.ai.sin.models.enums.VacancyEmploymentTypeEnum employmentType,
            Long specialityId,
            List<Long> skillIds,
            LocalDateTime publishedFrom,
            LocalDateTime publishedTo,
            Integer slotsCount,
            Boolean visibleToAnonymous
    ) {
        v.setTitle(title.trim());
        v.setDescription(description);
        v.setCity(StringUtils.hasText(city) ? city.trim() : null);
        v.setWorkFormat(workFormat);
        v.setEmploymentType(employmentType);
        if (specialityId != null) {
            SpecialityEnt speciality = specialityTools.getSpecialityOrThrow(specialityId);
            v.setSpeciality(speciality);
        } else {
            v.setSpeciality(null);
        }
        v.getSkills().clear();
        v.getSkills().addAll(resolveSkillsByIds(skillIds));
        v.setPublishedFrom(publishedFrom);
        v.setPublishedTo(publishedTo);
        v.setSlotsCount(slotsCount);
        if (visibleToAnonymous != null) {
            v.setVisibleToAnonymous(visibleToAnonymous);
        }
    }

    private Set<SkillEnt> resolveSkillsByIds(List<Long> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> unique = new HashSet<>(skillIds);
        Set<SkillEnt> found = skillRepo.findAllByIdIn(unique);
        if (found.size() != unique.size()) {
            throw new BadRequestException("Некоторые навыки не найдены по id");
        }
        return found;
    }

    private void validateReadyForReview(VacancyEnt v) {
        if (!StringUtils.hasText(v.getTitle())) {
            throw new BadRequestException("Укажите заголовок вакансии");
        }
        if (!StringUtils.hasText(v.getDescription()) || v.getDescription().trim().length() < MIN_DESCRIPTION_LENGTH) {
            throw new BadRequestException("Описание должно быть не короче " + MIN_DESCRIPTION_LENGTH + " символов");
        }
    }

    private VacancyCardDTO toCardDto(VacancyEnt v, UUID studentId) {
        String summary = v.getDescription();
        if (summary != null && summary.length() > 300) {
            summary = summary.substring(0, 300) + "…";
        }
        Boolean hasApplied = studentId != null
                && vacancyRepo.existsApplicationByVacancyAndStudent(v.getId(), studentId);
        return new VacancyCardDTO(
                v.getId(),
                v.getTitle(),
                summary,
                v.getCompanyName(),
                v.getCity(),
                v.getWorkFormat(),
                v.getEmploymentType(),
                v.getSpeciality() != null ? v.getSpeciality().getName() : null,
                vacancyRepo.countApplicationsByVacancyId(v.getId()),
                hasApplied,
                v.getPublishedFrom()
        );
    }

    private VacancyDTO toDto(VacancyEnt v, UUID studentId) {
        List<SkillDTO> skills = v.getSkills().stream()
                .sorted(SkillOrder.byCreatedAtThenId())
                .map(skillMapper::toDTO)
                .toList();
        Boolean hasApplied = studentId != null
                ? vacancyRepo.existsApplicationByVacancyAndStudent(v.getId(), studentId)
                : null;
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
                hasApplied,
                ts != null ? ts.getCreatedAt() : null,
                v.getManualSortOrder(),
                v.isVisibleToAnonymous()
        );
    }
}
