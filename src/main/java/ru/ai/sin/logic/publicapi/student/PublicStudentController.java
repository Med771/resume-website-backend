package ru.ai.sin.logic.publicapi.student;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ai.sin.logic.student.dto.FilterStudentReq;
import ru.ai.sin.logic.student.dto.StudentCardDTO;
import ru.ai.sin.models.PageResponse;

import java.util.UUID;

@RestController
@RequestMapping("/public/students")
@RequiredArgsConstructor
@Tag(
        name = "PublicStudents",
        description = """
                Публичная витрина карточек **без JWT и без cookie** (`permitAll`).
                В выдачу попадают только студенты с `publicProfileConsent=true`, `catalogVisible=true` и прочими правилами каталога.
                Контакты и прочие PII в `StudentCardDTO` не расширяются специально для анонимов — см. описание полей DTO.""")
public class PublicStudentController {

    private final PublicCatalogStudentService publicCatalogStudentService;

    @Operation(
            summary = "Карточка студента для витрины (по UUID)",
            description = """
                    Возвращает **укороченную** карточку (`StudentCardDTO`) для лендинга/каталога без авторизации.

                    **Успех (200):** студент найден, `publicProfileConsent=true`, `catalogVisible=true`.

                    **Ошибки:**
                    - **404** — нет записи, нет согласия на публичный показ, карточка скрыта (`catalogVisible=false`) или иные правила каталога.

                    Параметр сортировки из query **не** используется; пагинация здесь не применяется.""")
    @GetMapping("/{id}")
    public ResponseEntity<StudentCardDTO> getById(
            @Parameter(description = "UUID карточки студента", required = true, example = "00000000-0000-0000-0000-000000000001")
            @PathVariable UUID id) {
        return ResponseEntity.ok(publicCatalogStudentService.getCardById(id));
    }

    @Operation(
            summary = "Список карточек для витрины (постранично)",
            description = """
                    Тело — тот же `FilterStudentReq`, что и у `POST /student/cardsFilter` (фильтры по строке поиска, курсам, навыкам и т.д.).
                    Дополнительно сервер **всегда** накладывает условие: `publicProfileConsent=true` и `catalogVisible=true`.

                    **Пагинация:** стандартные query-параметры Spring Data — `page`, `size` (например `?page=0&size=20`).

                    **Сортировка:** задаётся **только** полями `sortBy`, `sortDirection`, `useDefaultRanking` в JSON.
                    Параметр `sort` в query **игнорируется** (защита от нестабильного API и sort injection).

                    Тело можно опустить (`null`) — тогда используется пустой фильтр (только публичные ограничения и пагинация).

                    **Ошибки:** **400** при невалидном JSON/ограничениях валидации DTO.""")
    @PostMapping("/cards")
    public ResponseEntity<PageResponse<StudentCardDTO>> listCards(
            @PageableDefault Pageable pageable,
            @Valid @RequestBody(required = false) FilterStudentReq filterStudentReq
    ) {
        FilterStudentReq filter = filterStudentReq == null ? emptyFilter() : filterStudentReq;
        return ResponseEntity.ok(publicCatalogStudentService.listCards(pageable, filter));
    }

    private static FilterStudentReq emptyFilter() {
        return new FilterStudentReq(null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
