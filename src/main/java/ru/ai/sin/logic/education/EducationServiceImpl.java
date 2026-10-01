package ru.ai.sin.logic.education;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import ru.ai.sin.models.PageResponse;

import ru.ai.sin.exception.models.BadRequestException;

import ru.ai.sin.helper.SecurityHelper;

import ru.ai.sin.logic.education.dto.*;

import ru.ai.sin.tools.EducationTools;


@Slf4j
@Service
@RequiredArgsConstructor
public class EducationServiceImpl implements EducationService {

    private final EducationRepo educationRepo;

    private final EducationMapper educationMapper;

    private final EducationTools educationTools;

    private final SecurityHelper securityHelper;

    @Override
    @Transactional(readOnly = true)
    public EducationDTO getById(long id) {
        return educationMapper.toDTO(educationTools.getEducationOrThrow(id));
    }

    @Override
    public PageResponse<EducationDTO> getAllByFilter(Pageable pageable, FilterEducationReq filterEducationReq) {
        Page<EducationEnt> page = educationRepo.findAll(
                EducationSpecifications.byFilters(filterEducationReq),
                pageable);

        return new PageResponse<>(
                page.getContent().stream().map(educationMapper::toDTO).toList(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Override
    public EducationDTO create(AddEducationReq addEducationReq) {
        EducationEnt educationEnt = educationMapper.toEntity(addEducationReq);

        try {
            educationEnt = educationRepo.save(educationEnt);
        }
        catch (DataIntegrityViolationException ex) {
            log.warn("Education already exists");

            throw new BadRequestException("Education already exists");
        }

        EducationDTO educationDTO = educationMapper.toDTO(educationEnt);

        log.info("User: {}, created a new education: {}", securityHelper.getCurrentUsername(), educationDTO);

        return educationDTO;
    }

    @Override
    @Transactional
    public EducationDTO update(
            long id,
            UpdateEducationReq updateEducationReq
    ) {
        EducationEnt educationEnt = educationTools.getEducationOrThrow(id);

        educationMapper.updateEntityFromDto(updateEducationReq, educationEnt);

        EducationDTO educationDTO = educationMapper.toDTO(educationEnt);

        log.info("User: {}, updated a education: {} with data: {}", securityHelper.getCurrentUsername(), id, educationDTO);

        return educationDTO;
    }

    @Override
    @Transactional
    public void deleteById(long id) {
        EducationEnt educationEnt = educationTools.getEducationOrThrow(id);

        try {
            educationRepo.delete(educationEnt);
        }
        catch (DataIntegrityViolationException ex) {
            log.warn("Error while deleting education: {}", ex.getMessage());

            throw new BadRequestException("Error while deleting education");
        }

        log.info("User: {}, deleted a education: {} with data: {}", securityHelper.getCurrentUsername(), id, educationEnt);
    }
}
