package ru.ai.sin.logic.vacancy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ru.ai.sin.models.enums.VacancyEmploymentTypeEnum;
import ru.ai.sin.models.enums.WorkFormatEnum;

import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "UpdateVacancyReq")
public record UpdateVacancyReq(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 16000) String description,
        @Size(max = 255) String city,
        WorkFormatEnum workFormat,
        VacancyEmploymentTypeEnum employmentType,
        @Positive Long specialityId,
        List<@Positive Long> skillIds,
        LocalDateTime publishedFrom,
        LocalDateTime publishedTo,
        Integer slotsCount,
        Boolean visibleToAnonymous
) {
}
