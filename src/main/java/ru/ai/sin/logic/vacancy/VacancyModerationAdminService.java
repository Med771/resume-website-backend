package ru.ai.sin.logic.vacancy;

import org.springframework.data.domain.Pageable;
import ru.ai.sin.logic.vacancy.dto.FilterVacancyModerationReq;
import ru.ai.sin.logic.vacancy.dto.PatchVacancyVitrinaReq;
import ru.ai.sin.logic.vacancy.dto.ReorderVacanciesReq;
import ru.ai.sin.logic.vacancy.dto.VacancyDTO;
import ru.ai.sin.logic.vacancy.dto.VacancyModerationRejectReq;
import ru.ai.sin.models.PageResponse;

import java.util.UUID;

public interface VacancyModerationAdminService {

    PageResponse<VacancyDTO> filter(Pageable pageable, FilterVacancyModerationReq filter);

    VacancyDTO getById(UUID id);

    VacancyDTO approve(UUID id);

    void reject(UUID id, VacancyModerationRejectReq body);

    void reorder(ReorderVacanciesReq req);

    VacancyDTO patchVitrina(UUID id, PatchVacancyVitrinaReq req);
}
