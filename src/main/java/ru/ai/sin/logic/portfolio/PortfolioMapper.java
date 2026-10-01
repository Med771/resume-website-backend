package ru.ai.sin.logic.portfolio;

import org.mapstruct.*;

import ru.ai.sin.logic.portfolio.dto.AddPortfolioReq;
import ru.ai.sin.logic.portfolio.dto.PortfolioDTO;

import ru.ai.sin.logic.student.StudentEnt;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PortfolioMapper {

    // ---------------- AddSpecialityReq -> PortfolioEnt ----------------
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamps", ignore = true)
    PortfolioEnt toEntity(AddPortfolioReq dto, StudentEnt student);

    // ---------------- PortfolioEnt -> PortfolioDTO ----------------
    @Mapping(target = "studentId", source = "entity.student.id")
    PortfolioDTO toDTO(PortfolioEnt entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamps", ignore = true)
    @Mapping(target = "student", ignore = true)
    void updateEntityFromDto(AddPortfolioReq dto, @MappingTarget PortfolioEnt entity);
}
