package ru.ai.sin.logic.recruiter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;

import org.springframework.data.web.PageableDefault;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;

import ru.ai.sin.models.PageResponse;

import ru.ai.sin.logic.recruiter.dto.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/recruiter")
@Tag(name = "Recruiter", description = "Операции управления рекрутерами")
public class RecruiterController {

    private final RecruiterService recruiterService;

    @Operation(
            summary = "Мой профиль рекрутера",
            description = "Возвращает рекрутера, привязанного к текущему пользователю. Если привязки ещё нет — 404 (нужно отправить первую заявку с полными данными).")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @GetMapping(path = "/me")
    public ResponseEntity<RecruiterDTO> getMyLinkedRecruiter() {
        return recruiterService
                .getLinkedForCurrentUser()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Получить рекрутера по ID", description = "Возвращает карточку рекрутера")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @GetMapping(path = "/{id}")
    public ResponseEntity<RecruiterDTO> getById(
            @PathVariable @NotNull UUID id
    ) {
        RecruiterDTO recruiterDTO = recruiterService.getById(id);

        return ResponseEntity.ok(recruiterDTO);
    }

    @Operation(
            summary = "Создать рекрутера",
            description = "Создаёт рекрутера без заявки. Если рекрутер с таким email уже есть — возвращается существующая запись (как при создании заявки)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping()
    public ResponseEntity<RecruiterDTO> create(@Valid @RequestBody AddRecruiterReq addRecruiterReq) {
        RecruiterDTO recruiterDTO = recruiterService.create(addRecruiterReq);
        return ResponseEntity.status(HttpStatus.CREATED).body(recruiterDTO);
    }

    @Operation(summary = "Фильтр рекрутеров", description = "Принимает DTO фильтра в request body и Pageable без параметра sort")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(path = "/filter")
    public ResponseEntity<PageResponse<RecruiterDTO>> filter(
            @PageableDefault Pageable pageable,

            @Valid @RequestBody FilterRecruiterReq filterRecruiterReq
    ) {
        PageResponse<RecruiterDTO> recruiterDTOs = recruiterService.getAllByFilter(pageable, filterRecruiterReq);

        return ResponseEntity.ok(recruiterDTOs);
    }

    @Operation(summary = "Обновить рекрутера", description = "Обновляет данные рекрутера по ID")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{id}")
    public ResponseEntity<RecruiterDTO> updateById(
            @PathVariable @NotNull UUID id,

            @Valid @RequestBody UpdateRecruiterReq recruiterReq
    ) {
        RecruiterDTO recruiterDTO = recruiterService.update(id, recruiterReq);

        return ResponseEntity.ok(recruiterDTO);
    }

    @Operation(
            summary = "Частично обновить рекрутера",
            description = "PATCH: в теле только изменяемые поля; отсутствующие или null не меняют значения в БД")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @PatchMapping(path = "/{id}")
    public ResponseEntity<RecruiterDTO> patchById(
            @PathVariable @NotNull UUID id,

            @Valid @RequestBody PatchRecruiterReq patchRecruiterReq
    ) {
        RecruiterDTO recruiterDTO = recruiterService.patch(id, patchRecruiterReq);

        return ResponseEntity.ok(recruiterDTO);
    }

    @Operation(summary = "Удалить рекрутера", description = "Удаляет рекрутера по ID")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable @NotNull UUID id) {
        recruiterService.deleteById(id);
    }
}
