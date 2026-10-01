package ru.ai.sin.logic.experience;

import org.springframework.data.domain.Pageable;

import ru.ai.sin.models.PageResponse;

import ru.ai.sin.logic.experience.dto.*;

public interface ExperienceService {

    // ---------- GET METHODS ----------
    ExperienceDTO getById(
            long id
    );

    // ---------- POST METHODS ----------
    PageResponse<ExperienceDTO> getAllByFilter(
            Pageable pageable,
            FilterExperienceReq filterExperienceReq);

    ExperienceDTO create(AddExperienceReq addExperienceReq);
    ExperienceDTO update(
            long id,
            UpdateExperienceReq updateExperienceReq);

    // ---------- DELETE METHODS ----------
    void deleteById(long id);
}
