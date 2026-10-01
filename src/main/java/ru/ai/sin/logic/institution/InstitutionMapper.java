package ru.ai.sin.logic.institution;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import ru.ai.sin.logic.institution.dto.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InstitutionMapper {

    // ---------------- AddInstitutionReq -> InstitutionEnt ----------------
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamps", ignore = true)
    @Mapping(target = "education", ignore = true)
    @Mapping(target = "student", ignore = true)
    InstitutionEnt toEntity(AddInstitutionReq dto);

    // ---------------- InstitutionEnt -> InstitutionDTO ----------------
    @Mapping(target = "educationId", source = "entity.education.id")
    @Mapping(target = "studentId", source = "entity.student.id")
    InstitutionDTO toDTO(InstitutionEnt entity, InstitutionRes institution);

    // ---------------- InstitutionEnt -> InstitutionRes ----------------
    InstitutionRes toRes(InstitutionEnt entity);

    // ---------------- UpdateInstitutionReq -> InstitutionEnt ----------------
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamps", ignore = true)
    @Mapping(target = "education", ignore = true)
    @Mapping(target = "student", ignore = true)
    void updateEntityFromDto(UpdateInstitutionReq dto, @MappingTarget InstitutionEnt entity);
}
