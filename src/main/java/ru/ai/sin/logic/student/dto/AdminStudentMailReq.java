package ru.ai.sin.logic.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(name = "AdminStudentMailReq")
public record AdminStudentMailReq(
        @Schema(description = "ID карточек студентов. Если список не пуст, all игнорируется")
        List<UUID> studentIds,

        @Schema(description = "Всем студентам, если studentIds пуст")
        Boolean all,

        @Schema(description = "Тема письма")
        @NotBlank(message = "Укажите тему")
        @Size(max = 200, message = "Тема должна быть короче 200 символов")
        String subject,

        @Schema(description = "Текст письма")
        @NotBlank(message = "Укажите текст")
        @Size(max = 5000, message = "Текст должен быть короче 5000 символов")
        String body
) {
    public boolean effectiveAll() {
        return Boolean.TRUE.equals(all);
    }
}
