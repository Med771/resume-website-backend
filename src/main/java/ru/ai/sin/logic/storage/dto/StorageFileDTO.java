package ru.ai.sin.logic.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "StorageFileDTO", description = "Файл изображения в хранилище приложения.")
public record StorageFileDTO(
        @Schema(description = "Имя файла (ключ для /main/photo/{fileName})")
        String fileName,
        @Schema(description = "Размер в байтах")
        long sizeBytes,
        @Schema(description = "Дата изменения")
        Instant lastModified
) {
}
