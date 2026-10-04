package ru.ai.sin.logic.siteproject;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.ai.sin.logic.siteproject.dto.CreateSiteProjectReq;
import ru.ai.sin.logic.siteproject.dto.FilterSiteProjectReq;
import ru.ai.sin.logic.siteproject.dto.ReorderSiteProjectsReq;
import ru.ai.sin.logic.siteproject.dto.SiteProjectDTO;
import ru.ai.sin.logic.siteproject.dto.SiteProjectStudentsReq;
import ru.ai.sin.logic.siteproject.dto.UpdateSiteProjectReq;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Tag(
        name = "Projects",
        description = """
                Один ресурс проектов сайта (`/projects`).
                Чтение: **STUDENT**, **RECRUITER**, **ADMIN**. CUD, reorder и привязка студентов: только **ADMIN**.
                Анонимная главная — `GET /public/vitrina/home` (тот же сервисный list, без HTTP на `/projects`).""")
public class SiteProjectController {

    private final SiteProjectService siteProjectService;

    @Operation(
            summary = "Фильтр проектов",
            description = """
                    **200** — массив `SiteProjectDTO` в порядке `sortOrder` (полный список, без пагинации).
                    Пустое или частичное тело — без ограничений по `q`/`section`/`visibleToAnonymous`.

                    **ADMIN** — все записи, с `students`. **RECRUITER** — окно публикации, с `students`.
                    **STUDENT** — окно публикации, `students = null`. **401** без входа.""")
    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<List<SiteProjectDTO>> filter(
            @Valid @RequestBody(required = false) FilterSiteProjectReq req) {
        return ResponseEntity.ok(siteProjectService.filter(req));
    }

    @Operation(
            summary = "Проект по id",
            description = """
                    **200** — `SiteProjectDTO`. Участники для **RECRUITER** и **ADMIN**.
                    **404** — нет записи или проект вне окна публикации для не-админа.""")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<SiteProjectDTO> getById(
            @Parameter(description = "UUID проекта", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(siteProjectService.getById(id));
    }

    @Operation(
            summary = "Создать проект",
            description = "Создаёт запись; `sortOrder` назначается сервером (в конец очереди). **201** + тело созданного DTO.")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SiteProjectDTO> create(@Valid @RequestBody CreateSiteProjectReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(siteProjectService.create(req));
    }

    @Operation(
            summary = "Обновить проект",
            description = "Полная замена полей по `id`, включая `images` (clear + insert). Не PATCH. **404**, если проект не найден.")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SiteProjectDTO> update(
            @Parameter(description = "UUID проекта", required = true) @PathVariable UUID id,
            @Valid @RequestBody UpdateSiteProjectReq req) {
        return ResponseEntity.ok(siteProjectService.update(id, req));
    }

    @Operation(summary = "Удалить проект", description = "**204** при успехе. **404**, если `id` не существует.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "UUID проекта", required = true) @PathVariable UUID id) {
        siteProjectService.delete(id);
    }

    @Operation(
            summary = "Задать порядок проектов",
            description = """
                    Тело: `orderedIds` — список UUID проектов в желаемом порядке: элемент с индексом `i` получит `sortOrder = i`.
                    Каждый id должен существовать; дубликаты в списке запрещены (**400**). Неупомянутые в списке проекты **не** меняют порядок автоматически.

                    **204** при успехе.""")
    @PostMapping("/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@Valid @RequestBody ReorderSiteProjectsReq req) {
        siteProjectService.reorder(req);
    }

    @Operation(
            summary = "Список студентов проекта",
            description = "UUID студентов, привязанных к проекту. **404**, если проект не найден.")
    @GetMapping("/{id}/students")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UUID>> listStudents(
            @Parameter(description = "UUID проекта", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(siteProjectService.listStudentIds(id));
    }

    @Operation(
            summary = "Привязать студентов к проекту",
            description = """
                    Добавляет связи many-to-many (повторная привязка игнорируется). **404**, если проект или любой студент не найден.
                    **400** при дубликатах в `studentIds`. **204** при успехе.""")
    @PostMapping("/{id}/students")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bindStudents(
            @Parameter(description = "UUID проекта", required = true) @PathVariable UUID id,
            @Valid @RequestBody SiteProjectStudentsReq req) {
        siteProjectService.bindStudents(id, req);
    }

    @Operation(
            summary = "Отвязать студентов от проекта",
            description = """
                    Удаляет связи; отсутствующие связи игнорируются. **404**, если проект не найден (студенты в списке не обязаны быть привязаны).
                    **400** при дубликатах в `studentIds`. **204** при успехе.""")
    @DeleteMapping("/{id}/students")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unbindStudents(
            @Parameter(description = "UUID проекта", required = true) @PathVariable UUID id,
            @Valid @RequestBody SiteProjectStudentsReq req) {
        siteProjectService.unbindStudents(id, req);
    }
}
