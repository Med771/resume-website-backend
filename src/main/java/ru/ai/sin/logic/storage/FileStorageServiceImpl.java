package ru.ai.sin.logic.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.FileHelper;
import ru.ai.sin.logic.storage.dto.StorageFileDTO;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final FileHelper fileHelper;

    @Override
    public List<StorageFileDTO> listImageFiles() {
        return fileHelper.listImageFiles().stream()
                .map(info -> new StorageFileDTO(info.fileName(), info.sizeBytes(), info.lastModified()))
                .toList();
    }

    @Override
    public StorageFileDTO uploadImage(MultipartFile file) {
        String savedName = fileHelper.uploadImage(file, UUID.randomUUID().toString());
        if (savedName == null || savedName.isBlank()) {
            throw new BadRequestException("Failed to save uploaded file");
        }
        return fileHelper.listImageFiles().stream()
                .filter(info -> info.fileName().equals(savedName))
                .findFirst()
                .map(info -> new StorageFileDTO(info.fileName(), info.sizeBytes(), info.lastModified()))
                .orElse(new StorageFileDTO(savedName, file.getSize(), null));
    }

    @Override
    public void deleteImage(String fileName) {
        try {
            fileHelper.deleteFile(fileName);
        } catch (FileNotFoundException e) {
            throw new NotFoundException("File not found: " + fileName);
        }
    }
}
