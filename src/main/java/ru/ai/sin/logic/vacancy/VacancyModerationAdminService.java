package ru.ai.sin.logic.vacancy;

import ru.ai.sin.logic.vacancy.dto.VacancyDTO;
import ru.ai.sin.logic.vacancy.dto.VacancyModerationRejectReq;
import ru.ai.sin.models.PageResponse;

import java.util.UUID;

@Deprecated
public interface VacancyModerationAdminService {

    PageResponse<VacancyDTO> list(int page, int size);

    VacancyDTO getById(UUID id);

    VacancyDTO approve(UUID id);

    void reject(UUID id, VacancyModerationRejectReq body);
}
