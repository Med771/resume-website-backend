package ru.ai.sin.logic.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(name = "AdminRecruiterMailResult")
public record AdminRecruiterMailResult(
        @Schema(description = "Почтовый сервер принял письмо")
        List<Sent> sent,
        @Schema(description = "Письмо не отправляли: NO_EMAIL, DUPLICATE, NOT_FOUND")
        List<Skipped> skipped,
        @Schema(description = "Отправка этому адресу не удалась")
        List<Failed> failed
) {
    @Schema(name = "AdminRecruiterMailSent")
    public record Sent(
            UUID recruiterId,
            String name,
            String email
    ) {
    }

    @Schema(name = "AdminRecruiterMailSkipped")
    public record Skipped(
            UUID recruiterId,
            String name,
            String email,
            String reason
    ) {
    }

    @Schema(name = "AdminRecruiterMailFailed")
    public record Failed(
            UUID recruiterId,
            String name,
            String email,
            String reason
    ) {
    }
}
