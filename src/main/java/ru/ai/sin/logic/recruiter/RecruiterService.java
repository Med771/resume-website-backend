package ru.ai.sin.logic.recruiter;

import org.springframework.data.domain.Pageable;

import ru.ai.sin.models.PageResponse;

import ru.ai.sin.logic.recruiter.dto.*;

import java.util.Optional;
import java.util.UUID;

public interface RecruiterService {

    // ---------- GET METHODS ----------
    RecruiterDTO getById(UUID id);

    /** Профиль рекрутера, привязанный к текущему пользователю (для фронта перед POST /request) */
    Optional<RecruiterDTO> getLinkedForCurrentUser();

    // ---------- POST METHODS ----------
    RecruiterDTO create(AddRecruiterReq addRecruiterReq);

    PageResponse<RecruiterDTO> getAllByFilter(
            Pageable pageable,
            FilterRecruiterReq filterRecruiterReq);

    // ---------- PUT METHODS ----------
    RecruiterDTO update(
            UUID id,
            UpdateRecruiterReq updateRecruiterReq);

    // ---------- PATCH METHODS ----------
    RecruiterDTO patch(UUID id, PatchRecruiterReq patchRecruiterReq);

    // ---------- DELETE METHODS ----------
    void deleteById(UUID id);
}
