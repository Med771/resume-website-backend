package ru.ai.sin.logic.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(
        name = "AnalyticsEventInReq",
        description = """
                Одно событие для записи через `POST /public/analytics/events`.
                Тип события должен быть из поддерживаемого набора на сервере (см. реализацию / enum); неизвестное значение — **400**.""")
public record AnalyticsEventInReq(
        @NotBlank
        @Size(max = 64)
        @Schema(
                description = "Логический тип события",
                example = "PAGE_VIEW",
                allowableValues = {"PAGE_VIEW"})
        String eventType,

        @NotBlank
        @Size(max = 1024)
        @Schema(
                description = "Путь страницы или стабильный ключ маршрута фронта",
                example = "/students/550e8400-e29b-41d4-a716-446655440000")
        String path,

        @Schema(
                description = """
                        Идентификатор анонимной сессии, генерируемый на фронте (UUID), чтобы склеивать просмотры без логина.
                        Для залогиненых пользователей сервер может дополнительно учитывать учётную запись — см. реализацию.""")
        UUID sessionId,

        @Size(max = 512)
        @Schema(description = "Необязательный заголовок User-Agent клиента (может обрезаться по длине)")
        String userAgent
) {
}
