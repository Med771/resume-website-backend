package ru.ai.sin.logic.vacancy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import ru.ai.sin.models.enums.VacancyEmploymentTypeEnum;
import ru.ai.sin.models.enums.WorkFormatEnum;

import java.util.List;
import java.util.Set;

@Schema(name = "FilterVacancyReq")
public record FilterVacancyReq(
        String findString,
        String city,
        Set<WorkFormatEnum> workFormats,
        Set<VacancyEmploymentTypeEnum> employmentTypes,
        List<@Positive Long> specialityIds,
        List<@Positive Long> skillIds
) {
}
