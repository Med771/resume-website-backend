package ru.ai.sin.logic.siteproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(name = "SiteProjectImageReq", description = "Изображение проекта при создании/обновении.")
public record SiteProjectImageReq(
        @Schema(description = "ID существующей записи (только при update); null — новая")
        UUID id,
        @Size(max = 512)
        @Schema(description = "Ключ файла в хранилище приложения")
        String imagePath,
        @Size(max = 1024)
        @Schema(description = "Прямая ссылка http/https")
        String imageUrl,
        @Schema(description = "Порядок в галерее")
        Integer sortOrder
) {
}
