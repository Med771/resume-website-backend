package ru.ai.sin.logic.experience;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import ru.ai.sin.models.PageResponse;

import ru.ai.sin.exception.models.BadRequestException;

import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.account.StudentReviewState;
import ru.ai.sin.logic.analytics.StudentProfileChangeRecorder;
import ru.ai.sin.logic.student.version.StudentProfileVersionService;

import ru.ai.sin.logic.experience.dto.*;
import ru.ai.sin.models.enums.ExperienceType;

import ru.ai.sin.tools.CompanyTools;
import ru.ai.sin.tools.ExperienceTools;

import ru.ai.sin.tools.StudentTools;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExperienceServiceImpl implements ExperienceService {

    private final ExperienceRepo experienceRepo;

    private final ExperienceMapper experienceMapper;

    private final ExperienceTools experienceTools;

    private final CompanyTools companyTools;
    private final StudentTools studentTools;

    private final SecurityHelper securityHelper;
    private final AccountAccessHelper accountAccessHelper;
    private final StudentReviewState studentReviewState;
    private final StudentProfileChangeRecorder studentProfileChangeRecorder;
    private final StudentProfileVersionService profileVersions;

    private void updateActiveCompanyOrThrow(long companyId, ExperienceEnt experienceEnt) {
        experienceEnt.setCompany(companyTools.getCompanyOrThrow(companyId));
    }

    private void updateActiveStudentOrThrow(UUID studentId, ExperienceEnt experienceEnt) {
        experienceEnt.setStudent(studentTools.getStudentOrThrow(studentId));
    }

    @Override
    @Transactional(readOnly = true)
    public ExperienceDTO getById(long id) {
        ExperienceEnt experienceEnt = experienceTools.getExperienceOrThrow(id);
        accountAccessHelper.requireCanReadStudentResumeDetails(experienceEnt.getStudent().getId());
        return experienceTools.mapToDTO(experienceEnt);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ExperienceDTO> getAllByFilter(Pageable pageable, FilterExperienceReq filterExperienceReq) {
        FilterExperienceReq effective = scopeResumeReadFilter(filterExperienceReq);
        Optional<PageResponse<ExperienceDTO>> drafted = profileVersions.draftExperiences(effective, pageable);
        if (drafted.isPresent()) {
            return drafted.get();
        }

        Page<ExperienceEnt> page = experienceRepo.findAll(
                ExperienceSpecifications.byFilters(effective),
                pageable);

        return new PageResponse<>(
                page.getContent().stream().map(experienceTools::mapToDTO).toList(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Override
    @Transactional
    public ExperienceDTO create(AddExperienceReq addExperienceReq) {
        UUID studentId = accountAccessHelper.resolveStudentIdForResumeMutation(addExperienceReq.studentId());
        accountAccessHelper.requireStudentCanMutateResume(studentId);
        requireValidDates(addExperienceReq.startDate(), addExperienceReq.endDate());
        Optional<ExperienceDTO> drafted = profileVersions.captureExperienceCreate(studentId, addExperienceReq);
        if (drafted.isPresent()) {
            studentProfileChangeRecorder.record(
                    studentId, "/experience", "experience", "create", drafted.get().experience().id(),
                    StudentProfileChangeRecorder.experienceFields(addExperienceReq));
            return drafted.get();
        }

        ExperienceEnt experienceEnt = experienceMapper.toEntity(addExperienceReq);
        if (experienceEnt.getExperienceType() == null) {
            experienceEnt.setExperienceType(ExperienceType.WORK);
        }

        updateActiveCompanyOrThrow(addExperienceReq.companyId(), experienceEnt);
        updateActiveStudentOrThrow(studentId, experienceEnt);

        try {
            experienceEnt = experienceRepo.save(experienceEnt);
        }
        catch (DataIntegrityViolationException ex) {
            log.warn("Experience already exists");

            throw new BadRequestException("Experience already exists");
        }

        studentReviewState.onStudentSelfEdit(experienceEnt.getStudent());
        recordExperienceChange(experienceEnt, "create", StudentProfileChangeRecorder.experienceFields(addExperienceReq));
        ExperienceDTO experienceDTO = experienceTools.mapToDTO(experienceEnt);

        log.info("User: {}, created a new experience: {}", securityHelper.getCurrentUsername(), experienceDTO);

        return experienceDTO;
    }

    @Override
    @Transactional
    public ExperienceDTO update(
            long id,
            UpdateExperienceReq updateExperienceReq
    ) {
        ExperienceEnt experienceEnt = experienceTools.getExperienceOrThrow(id);
        accountAccessHelper.requireStudentCanMutateResume(experienceEnt.getStudent().getId());
        requireValidDates(updateExperienceReq.startDate(), updateExperienceReq.endDate());
        Optional<ExperienceDTO> drafted = profileVersions.captureExperienceUpdate(
                experienceEnt.getStudent().getId(), id, updateExperienceReq);
        if (drafted.isPresent()) {
            studentProfileChangeRecorder.record(
                    experienceEnt.getStudent().getId(), "/experience", "experience", "update", id,
                    StudentProfileChangeRecorder.experienceFields(updateExperienceReq));
            return drafted.get();
        }

        experienceMapper.updateEntityFromDto(updateExperienceReq, experienceEnt);

        if (!Objects.equals(experienceEnt.getCompany().getId(), updateExperienceReq.companyId())) {
            updateActiveCompanyOrThrow(updateExperienceReq.companyId(), experienceEnt);
        }

        UUID targetStudentId = accountAccessHelper.resolveStudentIdForResumeMutation(
                updateExperienceReq.studentId() != null
                        ? updateExperienceReq.studentId()
                        : experienceEnt.getStudent().getId());
        if (!Objects.equals(experienceEnt.getStudent().getId(), targetStudentId)) {
            updateActiveStudentOrThrow(targetStudentId, experienceEnt);
        }

        studentReviewState.onStudentSelfEdit(experienceEnt.getStudent());
        recordExperienceChange(experienceEnt, "update", StudentProfileChangeRecorder.experienceFields(updateExperienceReq));
        ExperienceDTO experienceDTO = experienceTools.mapToDTO(experienceEnt);

        log.info("User: {}, updated a experience: {} with data: {}", securityHelper.getCurrentUsername(), id, experienceDTO);

        return experienceDTO;
    }

    @Override
    @Transactional
    public void deleteById(long id) {
        ExperienceEnt experienceEnt = experienceTools.getExperienceOrThrow(id);
        accountAccessHelper.requireStudentCanMutateResume(experienceEnt.getStudent().getId());
        if (profileVersions.captureExperienceDelete(experienceEnt.getStudent().getId(), id)) {
            studentProfileChangeRecorder.record(
                    experienceEnt.getStudent().getId(), "/experience", "experience", "delete", id, List.of());
            return;
        }

        try {
            experienceRepo.delete(experienceEnt);
        }
        catch (DataIntegrityViolationException ex) {
            log.warn("Error while deleting experience: {}", ex.getMessage());

            throw new BadRequestException("Error while deleting experience");
        }

        studentReviewState.onStudentSelfEdit(experienceEnt.getStudent());
        recordExperienceChange(experienceEnt, "delete", List.of());
        log.info("User: {}, deleted a experience: {} with data: {}", securityHelper.getCurrentUsername(), id, experienceEnt);
    }

    private void recordExperienceChange(ExperienceEnt experienceEnt, String action, List<String> fields) {
        if (experienceEnt.getStudent() == null) {
            return;
        }
        studentProfileChangeRecorder.record(
                experienceEnt.getStudent().getId(),
                "/experience",
                "experience",
                action,
                experienceEnt.getId(),
                fields);
    }

    private FilterExperienceReq scopeResumeReadFilter(FilterExperienceReq filter) {
        UUID requested = filter == null ? null : filter.studentId();
        UUID studentId = accountAccessHelper.resolveStudentIdForResumeRead(requested);
        accountAccessHelper.requireCanReadStudentResumeDetails(studentId);
        if (studentId != null && (filter == null || !studentId.equals(filter.studentId()))) {
            return new FilterExperienceReq(
                    studentId,
                    filter == null ? null : filter.companyId(),
                    filter == null ? null : filter.published());
        }
        return filter;
    }

    private static void requireValidDates(LocalDate startDate, LocalDate endDate) {
        if (endDate != null && startDate != null && endDate.isBefore(startDate)) {
            throw new BadRequestException("Дата окончания работы не может быть раньше даты начала");
        }
    }
}
