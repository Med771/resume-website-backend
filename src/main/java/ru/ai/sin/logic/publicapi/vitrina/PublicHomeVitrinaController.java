package ru.ai.sin.logic.publicapi.vitrina;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ai.sin.logic.publicapi.vitrina.dto.PublicHomeVitrinaDTO;

@RestController
@RequestMapping("/public/vitrina")
@RequiredArgsConstructor
@Tag(
        name = "PublicVitrina",
        description = """
                Публичная витрина главной страницы без JWT.
                Содержит только превью резюме и проектов; вакансии не включены.
                Полные каталоги доступны после регистрации и одобрения аккаунта.""")
public class PublicHomeVitrinaController {

    private final PublicHomeVitrinaService publicHomeVitrinaService;

    @Operation(
            summary = "Витрина главной страницы",
            description = """
                    Возвращает ограниченный набор карточек студентов и проектов для лендинга.
                    Лимиты задаются в `app.vitrina.home.students-limit` и `projects-limit`.""")
    @GetMapping("/home")
    public ResponseEntity<PublicHomeVitrinaDTO> getHome() {
        return ResponseEntity.ok(publicHomeVitrinaService.getHome());
    }
}
