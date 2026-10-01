package ru.ai.sin.logic.education;

import org.mapstruct.*;

import ru.ai.sin.logic.education.dto.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EducationMapper {

    // ---------------- AddEducationReq -> EducationEnt ----------------
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamps", ignore = true)
    @Mapping(target = "institutions", ignore = true)
    EducationEnt toEntity(AddEducationReq dto);

    // ---------------- EducationEnt -> EducationDTO ----------------
    EducationDTO toDTO(EducationEnt entity);

    // ---------------- UpdateEducationReq -> EducationEnt ----------------
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamps", ignore = true)
    @Mapping(target = "institutions", ignore = true)
    void updateEntityFromDto(UpdateEducationReq dto, @MappingTarget EducationEnt entity);
}
