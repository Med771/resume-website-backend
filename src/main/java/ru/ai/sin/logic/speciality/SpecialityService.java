package ru.ai.sin.logic.speciality;

import org.springframework.data.domain.Pageable;

import ru.ai.sin.models.PageResponse;

import ru.ai.sin.logic.speciality.dto.*;

public interface SpecialityService {

    // ---------- GET METHODS ----------
    SpecialityDTO getById(long id);

    // ---------- POST METHODS ----------
    PageResponse<SpecialityDTO> getAllByFilter(Pageable pageable, FilterSpecialityReq filterSpecialityReq);

    SpecialityDTO create(AddSpecialityReq addSpecialityReq);

    SpecialityDTO update(
            long id,
            UpdateSpecialityReq updateSpecialityReq);

    // ---------- POST METHODS ----------
    void deleteById(long id);
}
