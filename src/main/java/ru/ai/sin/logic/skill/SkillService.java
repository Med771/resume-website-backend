package ru.ai.sin.logic.skill;

import org.springframework.data.domain.Pageable;

import ru.ai.sin.models.PageResponse;

import ru.ai.sin.logic.skill.dto.*;

public interface SkillService {

    // ---------- GET METHODS ----------
    SkillDTO getById(long id);

    // ---------- POST METHODS ----------
    PageResponse<SkillDTO> getAllByFilter(
            Pageable pageable,
            FilterSkillReq filterSkillReq);

    SkillDTO create(AddSkillReq addSkillReq);

    SkillDTO updateById(
            long id,
            UpdateSkillReq updateSkillReq);

    // ---------- DELETE METHODS ----------
    void deleteById(long id);
}
