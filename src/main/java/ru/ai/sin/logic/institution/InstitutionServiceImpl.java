package ru.ai.sin.logic.institution;

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
import ru.ai.sin.models.enums.EducationType;

import ru.ai.sin.logic.institution.dto.*;

import ru.ai.sin.tools.EducationTools;
import ru.ai.sin.tools.InstitutionTools;
import ru.ai.sin.tools.StudentTools;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstitutionServiceImpl implements InstitutionService {

    private final InstitutionRepo institutionRepo;

    private final InstitutionMapper institutionMapper;

    private final InstitutionTools institutionTools;

    private final EducationTools educationTools;
    private final StudentTools studentTools;

    private final SecurityHelper securityHelper;
    private final AccountAccessHelper accountAccessHelper;
    private final StudentReviewState studentReviewState;
    private final StudentProfileChangeRecorder studentProfileChangeRecorder;
    private final StudentProfileVersionService profileVersions;

    private void updateActiveEducationOrThrow(long educationId, InstitutionEnt institutionEnt) {
        institutionEnt.setEducation(educationTools.getEducationOrThrow(educationId));
    }

    private void updateActiveStudentOrThrow(UUID studentId, InstitutionEnt institutionEnt) {
        institutionEnt.setStudent(studentTools.getStudentOrThrow(studentId));
    }

    @Override
    @Transactional(readOnly = true)
    public InstitutionDTO getById(long id) {
        InstitutionEnt institutionEnt = institutionTools.getInstitutionOrThrow(id);
        accountAccessHelper.requireCanReadStudentResumeDetails(institutionEnt.getStudent().getId());

        return institutionTools.mapToDTO(institutionEnt);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InstitutionDTO> getAllByFilter(Pageable pageable, FilterInstitutionReq filterInstitutionReq) {
        FilterInstitutionReq effective = scopeResumeReadFilter(filterInstitutionReq);
        Optional<PageResponse<InstitutionDTO>> drafted = profileVersions.draftEducation(effective, pageable);
        if (drafted.isPresent()) {
            return drafted.get();
        }

        Page<InstitutionEnt> page = institutionRepo.findAll(
                InstitutionSpecifications.byFilters(effective),
                pageable);

        return new PageResponse<>(
                page.getContent().stream().map(institutionTools::mapToDTO).toList(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Override
    @Transactional
    public InstitutionDTO create(AddInstitutionReq addInstitutionReq) {
        UUID studentId = accountAccessHelper.resolveStudentIdForResumeMutation(addInstitutionReq.studentId());
        accountAccessHelper.requireStudentCanMutateResume(studentId);
        Optional<InstitutionDTO> drafted = profileVersions.captureEducationCreate(studentId, addInstitutionReq);
        if (drafted.isPresent()) {
            studentProfileChangeRecorder.record(
                    studentId, "/institution", "education", "create", drafted.get().institution().id(),
                    StudentProfileChangeRecorder.institutionFields(addInstitutionReq));
            return drafted.get();
        }

        InstitutionEnt institutionEnt = institutionMapper.toEntity(addInstitutionReq);
        if (institutionEnt.getEducationType() == null) {
            institutionEnt.setEducationType(EducationType.STATE);
        }

        updateActiveEducationOrThrow(addInstitutionReq.educationId(), institutionEnt);
        updateActiveStudentOrThrow(studentId, institutionEnt);

        try {
            institutionEnt = institutionRepo.save(institutionEnt);
        }
        catch (DataIntegrityViolationException ex) {
            log.warn("Institution already exists");

            throw new BadRequestException("Institution already exists");
        }

        studentReviewState.onStudentSelfEdit(institutionEnt.getStudent());
        recordInstitutionChange(institutionEnt, "create", StudentProfileChangeRecorder.institutionFields(addInstitutionReq));
        InstitutionDTO institutionDTO = institutionTools.mapToDTO(institutionEnt);

        log.info("User: {}, created a new institution: {}", securityHelper.getCurrentUsername(), institutionDTO);

        return institutionDTO;
    }

    @Override
    @Transactional
    public InstitutionDTO update(
            long id,
            UpdateInstitutionReq updateInstitutionReq
    ) {
        InstitutionEnt institutionEnt = institutionTools.getInstitutionOrThrow(id);
        accountAccessHelper.requireStudentCanMutateResume(institutionEnt.getStudent().getId());
        Optional<InstitutionDTO> drafted = profileVersions.captureEducationUpdate(
                institutionEnt.getStudent().getId(), id, updateInstitutionReq);
        if (drafted.isPresent()) {
            studentProfileChangeRecorder.record(
                    institutionEnt.getStudent().getId(), "/institution", "education", "update", id,
                    StudentProfileChangeRecorder.institutionFields(updateInstitutionReq));
            return drafted.get();
        }

        institutionMapper.updateEntityFromDto(updateInstitutionReq, institutionEnt);

        if (!Objects.equals(institutionEnt.getEducation().getId(), updateInstitutionReq.educationId())) {
            updateActiveEducationOrThrow(updateInstitutionReq.educationId(), institutionEnt);
        }
        UUID targetStudentId = accountAccessHelper.resolveStudentIdForResumeMutation(
                updateInstitutionReq.studentId() != null
                        ? updateInstitutionReq.studentId()
                        : institutionEnt.getStudent().getId());
        if (!Objects.equals(institutionEnt.getStudent().getId(), targetStudentId)) {
            updateActiveStudentOrThrow(targetStudentId, institutionEnt);
        }

        studentReviewState.onStudentSelfEdit(institutionEnt.getStudent());
        recordInstitutionChange(institutionEnt, "update", StudentProfileChangeRecorder.institutionFields(updateInstitutionReq));
        InstitutionDTO institutionDTO = institutionTools.mapToDTO(institutionEnt);

        log.info("User: {}, updated a institution: {} with data: {}", securityHelper.getCurrentUsername(), id, institutionDTO);

        return institutionDTO;
    }

    @Override
    @Transactional
    public void deleteById(long id) {
        InstitutionEnt institutionEnt = institutionTools.getInstitutionOrThrow(id);
        accountAccessHelper.requireStudentCanMutateResume(institutionEnt.getStudent().getId());
        if (profileVersions.captureEducationDelete(institutionEnt.getStudent().getId(), id)) {
            studentProfileChangeRecorder.record(
                    institutionEnt.getStudent().getId(), "/institution", "education", "delete", id, List.of());
            return;
        }

        try {
            institutionRepo.delete(institutionEnt);
        }
        catch (DataIntegrityViolationException ex) {
            log.warn("Error while deleting institution: {}", ex.getMessage());

            throw new BadRequestException("Error while deleting institution");
        }

        studentReviewState.onStudentSelfEdit(institutionEnt.getStudent());
        recordInstitutionChange(institutionEnt, "delete", List.of());
        log.info("User: {}, deleted a institution: {} with data: {}", securityHelper.getCurrentUsername(), id, institutionEnt);
    }

    private void recordInstitutionChange(InstitutionEnt institutionEnt, String action, List<String> fields) {
        if (institutionEnt.getStudent() == null) {
            return;
        }
        studentProfileChangeRecorder.record(
                institutionEnt.getStudent().getId(),
                "/institution",
                "education",
                action,
                institutionEnt.getId(),
                fields);
    }

    private FilterInstitutionReq scopeResumeReadFilter(FilterInstitutionReq filter) {
        UUID requested = filter == null ? null : filter.studentId();
        UUID studentId = accountAccessHelper.resolveStudentIdForResumeRead(requested);
        accountAccessHelper.requireCanReadStudentResumeDetails(studentId);
        if (studentId != null && (filter == null || !studentId.equals(filter.studentId()))) {
            return new FilterInstitutionReq(
                    studentId,
                    filter == null ? null : filter.educationId(),
                    filter == null ? null : filter.published());
        }
        return filter;
    }
}
