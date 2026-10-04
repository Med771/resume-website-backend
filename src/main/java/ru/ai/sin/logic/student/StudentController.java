package ru.ai.sin.logic.student;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import ru.ai.sin.exception.models.NotFoundException;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import ru.ai.sin.models.PageResponse;

import ru.ai.sin.logic.student.dto.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/student")
@Tag(
        name = "Student",
        description = """
                Управление карточками студентов для **вошедших** пользователей (роли см. на методах).
                Публичная витрина главной — `GET /public/vitrina/home` (без входа).

                **Сортировка списков** (`POST /student/cardsFilter`, `POST /student/filter`): порядок задаётся полями `FilterStudentReq.sortBy`, `sortDirection`, `useDefaultRanking`;
                параметр query `sort` **игнорируется**.

                **Согласие на показ анонимам** (`publicProfileConsent`) и **ручной номер** (`manualSortOrder`): при **`POST /student`** и **`POST /student/extended`** можно задать сразу в теле; дальше — `PUT`/`PATCH` (сброс номера — `clearManualSortOrder: true`).""")
public class StudentController {

    private final StudentService studentService;

    @Operation(
            summary = "Текущий студент (ЛК)",
            description = """
                    Только **STUDENT**. Возвращает полный `StudentDTO` карточки, привязанной к текущему пользователю.

                    **404** — к учётной записи не привязана карточка студента.""")
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping(path = "/me")
    public ResponseEntity<StudentDTO> getMe() {
        return ResponseEntity.ok(studentService.getLinkedForCurrentUser()
                .orElseThrow(() -> new NotFoundException("К аккаунту не привязана карточка студента")));
    }

    @Operation(
            summary = "Обновить свою карточку",
            description = """
                    Только **STUDENT**. Частичный PATCH: `null` — поле не менять.
                    Навыки — только существующие `skillsIds`. Видимость в каталоге рекрутёров (`catalogVisible`) здесь не меняется.""")
    @PreAuthorize("hasRole('STUDENT')")
    @PatchMapping(path = "/me")
    public ResponseEntity<StudentDTO> patchMe(@Valid @RequestBody PatchStudentMeReq req) {
        return ResponseEntity.ok(studentService.patchMe(req));
    }

    @Operation(
            summary = "Получить студента по UUID",
            description = """
                    **STUDENT**, **RECRUITER** или **ADMIN** с одобренным аккаунтом. Полная карточка `StudentDTO`.

                    Карточки с `catalogVisible=false` для не-админов возвращают **404** (как при отсутствии id).

                    Требуется аутентификация по cookie/JWT и статус аккаунта **APPROVED** (кроме ADMIN).""")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @GetMapping(path = "/{id}")
    public ResponseEntity<StudentDTO> getById(@PathVariable @NotNull UUID id) {
        StudentDTO studentDTO = studentService.getById(id);

        return ResponseEntity.ok(studentDTO);
    }

    @Operation(
            summary = "Фильтр карточек студентов (укороченный DTO)",
            description = """
                    Постраничная выдача `StudentCardDTO` по фильтрам из тела.

                    **Скрытые карточки** (`catalogVisible=false`): в списке только для **ADMIN**; для остальных ролей отфильтровываются.

                    **Пагинация:** `page`, `size` в query. **Сортировка:** только из JSON (`sortBy`, `sortDirection`, `useDefaultRanking`), не из `sort=`.

                    Требуется статус аккаунта **APPROVED** (кроме ADMIN).""")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @PostMapping(path = "/cardsFilter")
    public ResponseEntity<PageResponse<StudentCardDTO>> getCardsAllByFilters(
            @PageableDefault Pageable pageable,

            @Valid @RequestBody FilterStudentReq filterStudentReq
    ) {
        PageResponse<StudentCardDTO> studentCardDTOs = studentService.getAllCardsByFilter(pageable, filterStudentReq);

        return ResponseEntity.ok(studentCardDTOs);
    }

    @Operation(
            summary = "Фильтр студентов (полный DTO)",
            description = """
                    Как `POST /student/cardsFilter`, но элементы страницы — полные `StudentDTO` (включая `publicProfileConsent`, `profileTextScore`).

                    Правила видимости, пагинации и сортировки — те же. Требуется **APPROVED** (кроме ADMIN).""")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @PostMapping(path = "/filter")
    public ResponseEntity<PageResponse<StudentDTO>> getAllByFilters(
            @PageableDefault Pageable pageable,

            @Valid @RequestBody FilterStudentReq filterStudentReq
    ) {
        PageResponse<StudentDTO> studentDTOs = studentService.getAllByFilter(pageable, filterStudentReq);

        return ResponseEntity.ok(studentDTOs);
    }

    @Operation(
            summary = "Загрузить или заменить фото студента",
            description = """
                    **STUDENT** — только своей карточки; **ADMIN** — любой. Часть `multipart/form-data`, имя части файла: **`avatarFile`**.

                    После успешной загрузки пересчитывается `profileTextScore` и может измениться порядок в релевантной сортировке.

                    **204** при успехе. **404** — нет студента. **400** — неверный формат/файл. **403** — чужой профиль.""")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @PostMapping(path = "/photo/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPhoto(
            @PathVariable @NotNull UUID id,

            @RequestPart("avatarFile") MultipartFile multipartFile
    ) {
        studentService.setPhoto(id, multipartFile);
    }

    @Operation(
            summary = "Создать студента (базовый набор полей)",
            description = """
                    Только **ADMIN**. Создаёт карточку и связи по DTO.

                    Опционально в теле: **`publicProfileConsent`** (по умолчанию как `false`, если не передано или `false`) и **`manualSortOrder`** (ручной порядок в каталоге; `null` — не задавать).

                    **201** + `StudentDTO`.""")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping()
    public ResponseEntity<StudentDTO> create(@Valid @RequestBody AddStudentReq addStudentReq) {
        StudentDTO studentDTO = studentService.create(addStudentReq);

        return ResponseEntity.status(HttpStatus.CREATED).body(studentDTO);
    }

    @Operation(
            summary = "Создать студента с вложенными сущностями",
            description = """
                    Только **ADMIN**. Атомарно создаёт студента, опционально портфолио, опыт, учебные заведения и навыки по вложенным спискам.

                    Поля **`publicProfileConsent`** и **`manualSortOrder`** — как при `POST /student` (опционально).

                    **201** + `StudentDTO`.""")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(path = "/extended")
    public ResponseEntity<StudentDTO> createExtended(@Valid @RequestBody CreateStudentExtendedReq createStudentExtendedReq) {
        StudentDTO studentDTO = studentService.createExtended(createStudentExtendedReq);

        return ResponseEntity.status(HttpStatus.CREATED).body(studentDTO);
    }

    @Operation(
            summary = "Полное обновление карточки студента (PUT)",
            description = """
                    Только **ADMIN**. Перезаписывает поля согласно `UpdateStudentReq`.

                    Поле `publicProfileConsent`: если передано **null** — текущее значение в БД **не меняется**; если `true`/`false` — устанавливается явно
                    (влияет на попадание карточки в `/public/students/...` при прочих условиях).

                    После сохранения пересчитывается `profileTextScore`.

                    **200** — актуальный `StudentDTO`. **404** — студент не найден.""")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{id}")
    public ResponseEntity<StudentDTO> updateById(
            @PathVariable @NotNull UUID id,

            @Valid @RequestBody UpdateStudentReq updateStudentReq
    ) {
        StudentDTO studentDTO = studentService.update(id, updateStudentReq);

        return ResponseEntity.ok(studentDTO);
    }

    @Operation(
            summary = "Частичное обновление карточки (PATCH)",
            description = """
                    Только **ADMIN**. В теле передаются **только** поля, которые нужно изменить; `null` у поля означает «не трогать».

                    Для `publicProfileConsent` действует то же правило: **null** — не менять, иначе выставить boolean.

                    Пересчёт `profileTextScore` выполняется при любом успешном PATCH.

                    **200** — `StudentDTO`. **404** — нет студента с таким `id`.""")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping(path = "/{id}")
    public ResponseEntity<StudentDTO> patchById(
            @PathVariable @NotNull UUID id,

            @Valid @RequestBody PatchStudentReq patchStudentReq
    ) {
        StudentDTO studentDTO = studentService.patch(id, patchStudentReq);

        return ResponseEntity.ok(studentDTO);
    }

    @Operation(
            summary = "Удалить студента",
            description = "Полное удаление: заявки, чаты (и сообщения), опыт работы, записи institution, портфолио, "
                    + "отвязка навыков; затем запись студента. Связь users.student_id снимается на стороне БД (ON DELETE SET NULL).")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable @NotNull UUID id) {
       studentService.deleteById(id);
    }
}
