package ru.ai.sin.logic.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        enumAsRef = true,
        description = "Направление сортировки для явного режима (`useDefaultRanking=false`). Если null — используется DESC.")
public enum StudentSortDirection {
    @Schema(description = "По возрастанию")
    ASC,
    @Schema(description = "По убыванию")
    DESC
}
