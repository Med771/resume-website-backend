package ru.ai.sin.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileConfig {
    @Value("${app.file.path}")
    private String filePath;

    @Getter
    @Value("${app.file.max-size-bytes:5242880}")
    private long maxUploadSizeBytes;

    public Path getFilePath() {
        if (maxUploadSizeBytes <= 0) {
            throw new IllegalStateException("app.file.max-size-bytes must be > 0");
        }

        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);

                System.out.println("Директория создана: " + path.toAbsolutePath());
            } catch (IOException e) {
                throw new IllegalStateException("Ошибка при создании директории: " + path.toAbsolutePath(), e);
            }
        }

        return path;
    }

}
