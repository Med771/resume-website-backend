package ru.ai.sin.logic.storage;

import org.springframework.web.multipart.MultipartFile;
import ru.ai.sin.logic.storage.dto.StorageFileDTO;

import java.util.List;

public interface FileStorageService {

    List<StorageFileDTO> listImageFiles();

    StorageFileDTO uploadImage(MultipartFile file);

    void deleteImage(String fileName);
}
