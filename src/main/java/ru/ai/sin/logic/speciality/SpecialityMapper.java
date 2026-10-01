package ru.ai.sin.logic.speciality;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import ru.ai.sin.logic.speciality.dto.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SpecialityMapper {

    // ---------------- AddSpecialityReq -> SpecialityEnt ----------------
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamps", ignore = true)
    SpecialityEnt toEntity(AddSpecialityReq dto);

    // ---------------- SpecialityEnt -> SpecialityDTO ----------------
    SpecialityDTO toDTO(SpecialityEnt entity);

    // ---------------- UpdateSpecialityReq -> SpecialityEnt ----------------
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamps", ignore = true)
    void updateEntityFromDto(UpdateSpecialityReq dto, @MappingTarget SpecialityEnt entity);
}