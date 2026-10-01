package ru.ai.sin.logic.storage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.ai.sin.logic.storage.dto.StorageFileDTO;

import java.util.List;

@RestController
@RequestMapping("/admin/storage/files")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "FileStorage", description = "Просмотр и загрузка изображений в файловое хранилище (только ADMIN).")
public class AdminFileStorageController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "Список изображений в хранилище")
    @GetMapping
    public ResponseEntity<List<StorageFileDTO>> list() {
        return ResponseEntity.ok(fileStorageService.listImageFiles());
    }

    @Operation(
            summary = "Загрузить изображение",
            description = "multipart/form-data, часть **`file`**. Возвращает имя файла для `imagePath` в проектах и др.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StorageFileDTO> upload(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fileStorageService.uploadImage(file));
    }

    @Operation(summary = "Удалить файл из хранилища")
    @DeleteMapping("/{fileName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "Имя файла в хранилище", required = true)
            @PathVariable String fileName) {
        fileStorageService.deleteImage(fileName);
    }
}
