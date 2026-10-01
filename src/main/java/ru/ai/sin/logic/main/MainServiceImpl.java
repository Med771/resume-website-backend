package ru.ai.sin.logic.main;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import ru.ai.sin.helper.FileHelper;
import ru.ai.sin.exception.models.NotFoundException;

import java.io.FileNotFoundException;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MainServiceImpl implements MainService {

    private final FileHelper fileHelper;

    @Override
    public byte[] getFileContent(String fileName) {
        try {
            return fileHelper.getFileContent(fileName);
        } catch (FileNotFoundException e) {
            throw new NotFoundException("File not found");
        }
    }

    @Override
    public String getContentType(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return "application/octet-stream";
        }
        String lower = imagePath.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".bmp")) {
            return "image/bmp";
        }
        if (lower.endsWith(".avif")) {
            return "image/avif";
        }
        if (lower.endsWith(".heic") || lower.endsWith(".heif")) {
            return "image/heic";
        }
        if (lower.endsWith(".tif") || lower.endsWith(".tiff")) {
            return "image/tiff";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".jpe")
                || lower.endsWith(".jfif") || lower.endsWith(".pjpeg") || lower.endsWith(".pjp")) {
            return "image/jpeg";
        }

        return "image/jpeg";
    }
}
