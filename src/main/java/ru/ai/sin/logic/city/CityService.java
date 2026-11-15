package ru.ai.sin.logic.city;

import org.springframework.data.domain.Pageable;
import ru.ai.sin.logic.city.dto.AddCityReq;
import ru.ai.sin.logic.city.dto.CityDTO;
import ru.ai.sin.logic.city.dto.FilterCityReq;
import ru.ai.sin.logic.city.dto.UpdateCityReq;
import ru.ai.sin.models.PageResponse;

public interface CityService {

    CityDTO getById(long id);

    PageResponse<CityDTO> getAllByFilter(Pageable pageable, FilterCityReq filter);

    CityDTO create(AddCityReq addCityReq);

    CityDTO update(long id, UpdateCityReq updateCityReq);

    void deleteById(long id);
}
