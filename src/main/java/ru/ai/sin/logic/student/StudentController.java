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
import ru.ai.sin.logic.student.version.StudentProfileVersionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/student")
@Tag(
        name = "Student",
        description = """
                Управление карточками студентов для **вошедших** пользователей (роли см. на методах).

                **Сортировка списков** (`POST /student/cardsFilter`, `POST /student/filter`): порядок задаётся полями `FilterStudentReq.sortBy`, `sortDirection`, `useDefaultRanking`;
                параметр query `sort` **игнорируется**.

                Поле `publicProfileConsent` сохраняется в карточке. Каталог для вошедших его не читает.
                **Ручной номер** (`manualSortOrder`): при **`POST /student`** можно задать сразу в теле; дальше — `PUT`/`PATCH` (сброс номера — `clearManualSortOrder: true`).""")
public class StudentController {

    private final StudentService studentService;
    private final StudentProfileVersionService profileVersions;

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

    @Operation(summary = "Одобренная анкета, которую видит каталог")
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping(path = "/me/published")
    public ResponseEntity<StudentDTO> publishedMe() {
        return ResponseEntity.ok(profileVersions.publishedForCurrent());
    }

    @Operation(summary = "Свои версии анкеты: номер, статус и время")
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping(path = "/me/versions")
    public ResponseEntity<List<StudentProfileVersionItemDTO>> myVersions() {
        return ResponseEntity.ok(profileVersions.listForCurrent());
    }

    @Operation(summary = "Одна своя версия анкеты")
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping(path = "/me/versions/{number}")
    public ResponseEntity<StudentDTO> myVersion(@PathVariable int number) {
        return ResponseEntity.ok(profileVersions.versionForCurrent(number));
    }

    @Operation(summary = "Вернуть прошлую анкету в течение пяти минут")
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping(path = "/me/revert-draft")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revertDraft() {
        StudentDTO me = studentService.getLinkedForCurrentUser()
                .orElseThrow(() -> new NotFoundException("К аккаунту не привязана карточка студента"));
        profileVersions.revertDraft(me.id());
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
            summary = "Завершить регистрацию",
            description = """
                    Только **STUDENT** с подтверждённой почтой и статусом `PENDING_APPROVAL`.
                    Тела нет. Отправляет письмо о завершении регистрации.
                    Дата рождения, курс и город этим вызовом не меняются.""")
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping(path = "/me/complete-registration")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void completeRegistration() {
        studentService.completeRegistration();
    }

    @Operation(
            summary = "Отправить полную анкету на одобрение",
            description = """
                    Только **STUDENT** с подтверждённой почтой и статусом `PENDING_APPROVAL`.
                    Нужны хотя бы один опыт (`experience`), одно место учёбы (`education`) и одно портфолио (`portfolio`).
                    Если анкета неполная — 403 `PROFILE_INCOMPLETE`. Иначе письмо с просьбой дождаться одобрения.""")
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping(path = "/me/submit-for-review")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void submitForReview() {
        studentService.submitForReview();
    }

    @Operation(
            summary = "Получить студента по UUID",
            description = """
                    Полная карточка `StudentDTO`.

                    Студент и рекрутер с любым своим статусом видят чужую карточку, если у неё аккаунт **APPROVED** и `catalogVisible=true`. Иначе **404**.
                    Свою карточку студент видит всегда, даже если она скрыта или ещё не одобрена. Админ видит любую.""")
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

                    Студент и рекрутер с любым своим статусом видят студентов со статусом **APPROVED** и `catalogVisible=true`.
                    Скрытый и не одобренный в список не попадают. Своя ещё не одобренная карточка в список не входит: она на `GET /student/me`.
                    Одобренный студент видит свою карточку, даже если она скрыта. Админ видит всех.

                    **Пагинация:** `page`, `size` в query. **Сортировка:** только из JSON (`sortBy`, `sortDirection`, `useDefaultRanking`), не из `sort=`.""")
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

                    Правила видимости, пагинации и сортировки — те же, что у `POST /student/cardsFilter`.""")
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
            summary = "Удалить фото студента",
            description = """
                    **STUDENT** — только своей карточки; **ADMIN** — любой.
                    Стирает файл из хранилища и очищает `imagePath`. Если фото уже нет — **204**.
                    Правка своей карточки студентом возвращает анкету на проверку. Правка админом статус не меняет.

                    **204** при успехе. **404** — нет студента. **403** — чужой профиль.""")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @DeleteMapping(path = "/photo/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePhoto(@PathVariable @NotNull UUID id) {
        studentService.deletePhoto(id);
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
            summary = "Полное обновление карточки студента (PUT)",
            description = """
                    Только **ADMIN**. Перезаписывает поля согласно `UpdateStudentReq`.

                    Поле `publicProfileConsent`: если передано **null** — текущее значение в БД **не меняется**; если `true`/`false` — устанавливается явно.
                    Каталог для вошедших это поле не читает.

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
            description = "Полное удаление: отклики на вакансии, заявки, чаты (и сообщения), опыт работы, "
                    + "записи institution, портфолио, отвязка навыков; затем учётная запись (логин, пароль, "
                    + "почта регистрации) и карточка студента.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable @NotNull UUID id) {
       studentService.deleteById(id);
    }
}
