package ru.ai.sin.logic.company;

import org.mapstruct.*;
import ru.ai.sin.logic.company.dto.CompanyDTO;
import ru.ai.sin.logic.company.dto.CompanyRes;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CompanyMapper {

    // ---------------- CompanyEnt -> CompanyDTO ----------------
    CompanyDTO toDTO(CompanyEnt entity, List<Long> experiencesIds);

    // ---------------- CompanyEnt -> CompanyRes ----------------
    CompanyRes toRes(CompanyEnt entity);
}