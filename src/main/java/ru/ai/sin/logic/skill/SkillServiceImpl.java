package ru.ai.sin.logic.skill;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import ru.ai.sin.models.PageResponse;

import ru.ai.sin.logic.skill.dto.*;
import ru.ai.sin.logic.speciality.SpecialityEnt;

import ru.ai.sin.exception.models.BadRequestException;

import ru.ai.sin.helper.SecurityHelper;

import ru.ai.sin.tools.SkillTools;
import ru.ai.sin.tools.SpecialityTools;

import java.util.HashSet;
import java.util.Set;


@Slf4j
@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {

    private final SkillRepo skillRepo;

    private final SkillMapper skillMapper;

    private final SkillTools skillTools;

    private final SpecialityTools specialityTools;

    private final SecurityHelper securityHelper;

    @Override
    @Transactional(readOnly = true)
    public SkillDTO getById(long id) {
        return skillMapper.toDTO(skillTools.getSkillOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SkillDTO> getAllByFilter(Pageable pageable, FilterSkillReq filterSkillReq) {
        Pageable ordered = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "timestamps.createdAt", "id")
        );
        Page<SkillEnt> page = skillRepo.findAll(
                SkillSpecifications.byFilters(filterSkillReq),
                ordered
        );

        return new PageResponse<>(
                page.getContent().stream().map(skillMapper::toDTO).toList(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }


    @Override
    @Transactional
    public SkillDTO create(AddSkillReq addSkillReq) {
        if (addSkillReq.parentId() != null && addSkillReq.specialityId() != null) {
            throw new BadRequestException("Специальность задаётся только корню");
        }
        SkillEnt skillEnt = new SkillEnt(addSkillReq.name());
        if (addSkillReq.parentId() != null) {
            SkillEnt parent = skillTools.getSkillOrThrow(addSkillReq.parentId());
            assertNoCycle(null, parent);
            skillEnt.setParent(parent);
        }
        if (addSkillReq.specialityId() != null) {
            skillEnt.setSpeciality(requireSpeciality(addSkillReq.specialityId()));
        }

        try {
            skillEnt = skillRepo.save(skillEnt);
        }
        catch (DataIntegrityViolationException ex) {
            log.warn("Skill already exists: {}", addSkillReq.name());

            throw new BadRequestException("Skill already exists: " + addSkillReq.name());
        }

        SkillDTO skillDTO = skillMapper.toDTO(skillEnt);

        log.info("User: {}, created a new skill: {}", securityHelper.getCurrentUsername(), skillDTO);

        return skillDTO;
    }



    @Override
    @Transactional
    public SkillDTO updateById(
            long id,
            UpdateSkillReq updateSkillReq
    ) {
        SkillEnt skillEnt = skillTools.getSkillOrThrow(id);

        skillMapper.updateEntityFromDto(updateSkillReq, skillEnt);
        applyGraph(skillEnt, updateSkillReq);

        SkillDTO skillDTO = skillMapper.toDTO(skillEnt);

        log.info("User: {}, updated a skill: {} with data: {}", securityHelper.getCurrentUsername(), id, skillDTO);

        return skillDTO;
    }

    @Override
    @Transactional
    public void deleteById(long id) {
        SkillEnt skillEnt = skillTools.getSkillOrThrow(id);

        try {
            skillRepo.delete(skillEnt);
        }
        catch (DataIntegrityViolationException ex) {
            log.warn("Error while deleting skill: {}", ex.getMessage());

            throw new BadRequestException("Error while deleting skill");
        }

        log.info("User: {}, deleted a skill: {} with data: {}", securityHelper.getCurrentUsername(), id, skillEnt);
    }

    private void applyGraph(SkillEnt skill, UpdateSkillReq req) {
        boolean setParent = req.parentId() != null;
        boolean clearParent = Boolean.TRUE.equals(req.clearParent());
        boolean setSpeciality = req.specialityId() != null;
        boolean clearSpeciality = Boolean.TRUE.equals(req.clearSpeciality());
        if (setParent && clearParent) {
            throw new BadRequestException("Укажите либо parentId, либо clearParent");
        }
        if (setSpeciality && clearSpeciality) {
            throw new BadRequestException("Укажите либо specialityId, либо clearSpeciality");
        }
        if (setParent && setSpeciality) {
            throw new BadRequestException("Специальность задаётся только корню");
        }
        if (clearParent) {
            skill.setParent(null);
        }
        if (setParent) {
            if (req.parentId().equals(skill.getId())) {
                throw new BadRequestException("Навык не может быть родителем самого себя");
            }
            SkillEnt parent = skillTools.getSkillOrThrow(req.parentId());
            assertNoCycle(skill.getId(), parent);
            skill.setParent(parent);
            skill.setSpeciality(null);
        }
        if (clearSpeciality) {
            skill.setSpeciality(null);
        }
        if (setSpeciality) {
            if (skill.getParent() != null) {
                throw new BadRequestException("Специальность задаётся только корню");
            }
            skill.setSpeciality(requireSpeciality(req.specialityId()));
        }
    }

    private void assertNoCycle(Long skillId, SkillEnt parent) {
        Set<Long> seen = new HashSet<>();
        if (skillId != null) {
            seen.add(skillId);
        }
        SkillEnt current = parent;
        while (current != null) {
            if (!seen.add(current.getId())) {
                throw new BadRequestException("Такой родитель замыкает граф навыков");
            }
            current = current.getParent();
        }
    }

    private SpecialityEnt requireSpeciality(Long specialityId) {
        return specialityTools.getSpecialityOrThrow(specialityId);
    }
}
