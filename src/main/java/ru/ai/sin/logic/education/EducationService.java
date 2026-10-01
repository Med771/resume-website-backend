package ru.ai.sin.logic.education;

import org.springframework.data.domain.Pageable;

import ru.ai.sin.models.PageResponse;

import ru.ai.sin.logic.education.dto.*;

public interface EducationService {

    // ---------- GET METHODS ----------
    EducationDTO getById(long id);

    PageResponse<EducationDTO> getAllByFilter(
            Pageable pageable,
            FilterEducationReq filterEducationReq);

    // ---------- POST METHODS ----------
    EducationDTO create(AddEducationReq addEducationReq);

    EducationDTO update(
            long id,
            UpdateEducationReq updateEducationReq);

    // ---------- DELETE METHODS ----------
    void deleteById(long id);
}
