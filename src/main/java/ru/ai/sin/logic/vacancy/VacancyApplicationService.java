package ru.ai.sin.logic.vacancy;

import org.springframework.data.domain.Pageable;
import ru.ai.sin.logic.vacancy.dto.ApplyVacancyReq;
import ru.ai.sin.logic.vacancy.dto.RejectApplicationReq;
import ru.ai.sin.logic.vacancy.dto.VacancyApplicationDTO;
import ru.ai.sin.models.PageResponse;

import java.util.UUID;

public interface VacancyApplicationService {

    VacancyApplicationDTO apply(UUID vacancyId, ApplyVacancyReq req);

    PageResponse<VacancyApplicationDTO> listMine(Pageable pageable);

    void withdraw(UUID applicationId);

    PageResponse<VacancyApplicationDTO> listForVacancy(UUID vacancyId, Pageable pageable);

    VacancyApplicationDTO accept(UUID vacancyId, UUID applicationId);

    VacancyApplicationDTO reject(UUID vacancyId, UUID applicationId, RejectApplicationReq req);
}
