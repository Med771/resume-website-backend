package ru.ai.sin.logic.siteproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "SiteProjectImageDTO", description = "Изображение в галерее проекта.")
public record SiteProjectImageDTO(
        @Schema(description = "UUID записи изображения")
        UUID id,
        @Schema(description = "Ключ файла в хранилище")
        String imagePath,
        @Schema(description = "Внешняя ссылка; приоритетнее imagePath")
        String imageUrl,
        @Schema(description = "Порядок в галерее")
        int sortOrder
) {
}
