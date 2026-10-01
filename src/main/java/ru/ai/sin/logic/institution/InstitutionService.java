package ru.ai.sin.logic.institution;

import org.springframework.data.domain.Pageable;

import ru.ai.sin.models.PageResponse;

import ru.ai.sin.logic.institution.dto.*;


public interface InstitutionService {

    // ---------- GET METHODS ----------
    InstitutionDTO getById(long id);

    // ---------- POST METHODS ----------
    PageResponse<InstitutionDTO> getAllByFilter(Pageable pageable, FilterInstitutionReq filterInstitutionReq);

    InstitutionDTO create(AddInstitutionReq addInstitutionReq);
    InstitutionDTO update(
            long id,
            UpdateInstitutionReq updateInstitutionReq);

    // ---------- DELETE METHODS ----------
    void deleteById(long id);
}
