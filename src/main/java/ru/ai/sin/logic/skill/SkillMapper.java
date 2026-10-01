package ru.ai.sin.logic.skill;

import org.mapstruct.*;

import ru.ai.sin.logic.skill.dto.SkillDTO;
import ru.ai.sin.logic.skill.dto.UpdateSkillReq;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SkillMapper {

    // ---------------- SkillEnt -> SkillDTO ----------------
    SkillDTO toDTO(SkillEnt entity);

    // ---------------- UpdateSkillReq -> SkillEnt ----------------
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamps", ignore = true)
    void updateEntityFromDto(UpdateSkillReq dto, @MappingTarget SkillEnt entity);
}