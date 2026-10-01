package ru.ai.sin.tools;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.ai.sin.logic.institution.dto.InstitutionDTO;
import ru.ai.sin.logic.institution.dto.InstitutionRes;

import ru.ai.sin.logic.institution.InstitutionEnt;
import ru.ai.sin.logic.institution.InstitutionMapper;
import ru.ai.sin.logic.institution.InstitutionRepo;

import ru.ai.sin.exception.models.NotFoundException;


@Component
@RequiredArgsConstructor
public class InstitutionTools {

    private final InstitutionRepo institutionRepo;

    private final InstitutionMapper institutionMapper;

    @Transactional(readOnly = true)
    public InstitutionEnt getInstitutionOrThrow(long id) {
        InstitutionEnt institutionEnt = institutionRepo.findWithEducationAndStudentById(id);

        if (institutionEnt == null) {
            throw new NotFoundException("Failed to find institution with id: " + id);
        }

        return institutionEnt;
    }

    public InstitutionDTO mapToDTO(InstitutionEnt institutionEnt) {
        InstitutionRes institutionRes = institutionMapper.toRes(institutionEnt);

        return institutionMapper.toDTO(institutionEnt, institutionRes);
    }
}
