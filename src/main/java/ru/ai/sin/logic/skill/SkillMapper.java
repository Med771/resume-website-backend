package ru.ai.sin.logic.skill;

import org.mapstruct.*;

import ru.ai.sin.logic.skill.dto.SkillDTO;
import ru.ai.sin.logic.skill.dto.UpdateSkillReq;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SkillMapper {

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "specialityId", source = "speciality.id")
    SkillDTO toDTO(SkillEnt entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamps", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "speciality", ignore = true)
    void updateEntityFromDto(UpdateSkillReq dto, @MappingTarget SkillEnt entity);
}