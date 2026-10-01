package ru.ai.sin.logic.chat.dto;

import ru.ai.sin.logic.request.dto.RequestDTO;
import ru.ai.sin.logic.vacancy.dto.VacancyApplicationDTO;

import java.util.List;
import java.util.UUID;

public record ChatContextDTO(
        UUID chatId,
        ChatSummaryDTO summary,
        List<RequestDTO> requests,
        List<VacancyApplicationDTO> vacancyApplications
) {
}
