package ru.ai.sin.logic.vacancy;

import org.springframework.data.domain.Pageable;
import ru.ai.sin.logic.vacancy.dto.CreateVacancyReq;
import ru.ai.sin.logic.vacancy.dto.FilterVacancyReq;
import ru.ai.sin.logic.vacancy.dto.UpdateVacancyReq;
import ru.ai.sin.logic.vacancy.dto.VacancyCardDTO;
import ru.ai.sin.logic.vacancy.dto.VacancyDTO;
import ru.ai.sin.models.PageResponse;

import java.util.List;
import java.util.UUID;

public interface VacancyService {

    PageResponse<VacancyCardDTO> listPublishedFeed(Pageable pageable, FilterVacancyReq filter);

    VacancyDTO getById(UUID id);

    List<VacancyDTO> listMine();

    VacancyDTO create(CreateVacancyReq req);

    VacancyDTO update(UUID id, UpdateVacancyReq req);

    VacancyDTO submitForReview(UUID id);

    VacancyDTO close(UUID id);

    void delete(UUID id);
}
