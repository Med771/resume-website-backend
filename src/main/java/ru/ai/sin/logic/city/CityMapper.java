package ru.ai.sin.logic.city;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.ai.sin.logic.city.dto.AddCityReq;
import ru.ai.sin.logic.city.dto.CityDTO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamps", ignore = true)
    CityEnt toEntity(AddCityReq dto);

    CityDTO toDTO(CityEnt entity);
}
