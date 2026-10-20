package ru.ai.sin.logic.main;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.helper.FileHelper;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.student.StudentRepo;

import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MainServiceImpl implements MainService {

    private final FileHelper fileHelper;
    private final StudentRepo studentRepo;
    private final AccountAccessHelper accountAccessHelper;
    private final SecurityHelper securityHelper;

    @Override
    public byte[] getFileContent(String fileName) {
        requireCanRead(fileName);
        try {
            return fileHelper.getFileContent(fileName);
        } catch (FileNotFoundException e) {
            throw new NotFoundException("File not found");
        }
    }

    private void requireCanRead(String fileName) {
        Optional<StudentEnt> student = fileName == null
                ? Optional.empty()
                : studentRepo.findByImagePath(fileName);
        if (student.isPresent()) {
            accountAccessHelper.requireCanReadStudentResumeDetails(student.get().getId());
            return;
        }
        if (!securityHelper.isCurrentUserAdmin()) {
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
