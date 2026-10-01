package ru.ai.sin.logic.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(name = "CreateStudentSkillReq", description = "DTO навыка для расширенного создания студента")
public record CreateStudentSkillReq(
        @Schema(description = "ID существующего навыка")
        @Positive
        Long id,

        @Schema(description = "Название нового навыка (если id не передан)")
        @Size(min = 1, max = 255, message = "Skill name must be less than 255 characters")
        String name
) {
}
