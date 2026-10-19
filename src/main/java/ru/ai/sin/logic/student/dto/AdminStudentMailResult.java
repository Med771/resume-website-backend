package ru.ai.sin.logic.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(name = "AdminStudentMailResult")
public record AdminStudentMailResult(
        @Schema(description = "Почтовый сервер принял письмо")
        List<Sent> sent,
        @Schema(description = "Письмо не отправляли: NO_EMAIL, DUPLICATE, NOT_FOUND")
        List<Skipped> skipped,
        @Schema(description = "Отправка этому адресу не удалась")
        List<Failed> failed
) {
    @Schema(name = "AdminStudentMailSent")
    public record Sent(
            UUID studentId,
            String name,
            String email
    ) {
    }

    @Schema(name = "AdminStudentMailSkipped")
    public record Skipped(
            UUID studentId,
            String name,
            String email,
            String reason
    ) {
    }

    @Schema(name = "AdminStudentMailFailed")
    public record Failed(
            UUID studentId,
            String name,
            String email,
            String reason
    ) {
    }
}
