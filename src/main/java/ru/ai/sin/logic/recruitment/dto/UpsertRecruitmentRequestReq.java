package ru.ai.sin.logic.recruitment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ru.ai.sin.models.enums.CooperationFormat;

import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "UpsertRecruitmentRequestReq", description = "Создание и правка заявки работодателя на студентов.")
public record UpsertRecruitmentRequestReq(
        @NotBlank
        @Size(max = 8000)
        @Schema(description = "Для каких задач нужны студенты")
        String tasks,
        @NotBlank
        @Size(max = 8000)
        @Schema(description = "Основные требования к квалификации")
        String qualificationRequirements,
        @NotBlank
        @Size(max = 8000)
        @Schema(description = "Условия работы")
        String workConditions,
        @NotNull
        @Min(1)
        @Schema(description = "Сколько студентов нужно, целое число от 1")
        Integer studentCount,
        @Schema(description = "Формат сотрудничества. Можно не передавать")
        CooperationFormat cooperationFormat,
        @Schema(description = "Готовы запросить студентов. Можно не передавать")
        Boolean readyToRequestStudents,
        @Size(max = 8000)
        @Schema(description = "Сроки самой работы. Можно не передавать")
        String scheduleNote,
        @Size(max = 8000)
        @Schema(description = "Прочая информация. Можно не передавать")
        String extraInfo,
        @Schema(description = "Начало окна публикации. Пусто — без нижней границы")
        LocalDateTime publishedFrom,
        @Schema(description = "Конец окна публикации. Пусто — без верхней границы")
        LocalDateTime publishedTo,
        @NotEmpty
        @Schema(description = "Специальности из справочника")
        List<@NotNull @Positive Long> specialityIds,
        @NotEmpty
        @Schema(description = "Города из справочника")
        List<@NotNull @Positive Long> cityIds,
        @Size(max = 255)
        @Schema(description = "Название компании. Обязательно, когда заявку создаёт админ")
        String companyName
) {
}
