package ru.ai.sin.logic.main;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/main")
@Tag(name = "Main", description = "Системные и публичные endpoint'ы")
public class MainController {

    private final MainService mainService;

    @Operation(summary = "Проверка доступности сервиса", description = "Технический endpoint для проверки что backend работает")
    @GetMapping(path = "status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void status() {}

    @Operation(summary = "Получить фото", description = "Возвращает содержимое файла изображения по имени/пути")
    @GetMapping(path = "photo/{image_path}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable(name = "image_path") String imagePath) {
        byte[] bytes = mainService.getFileContent(imagePath);
        String contentType = mainService.getContentType(imagePath);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(bytes);
    }
}
