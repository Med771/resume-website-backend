package ru.ai.sin.logic.vacancy;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.ai.sin.logic.vacancy.dto.FilterVacancyModerationReq;
import ru.ai.sin.logic.vacancy.dto.PatchVacancyVitrinaReq;
import ru.ai.sin.logic.vacancy.dto.ReorderVacanciesReq;
import ru.ai.sin.logic.vacancy.dto.VacancyDTO;
import ru.ai.sin.logic.vacancy.dto.VacancyModerationRejectReq;
import ru.ai.sin.models.PageResponse;

import java.util.UUID;

@RestController
@RequestMapping("/admin/vacancies")
@RequiredArgsConstructor
@Validated
@Tag(name = "VacancyModerationAdmin", description = "Модерация вакансий перед публикацией")
@PreAuthorize("hasRole('ADMIN')")
public class VacancyAdminController {

    private final VacancyModerationAdminService vacancyModerationAdminService;

    @Operation(summary = "Очередь модерации вакансий")
    @PostMapping("/filter")
    public ResponseEntity<PageResponse<VacancyDTO>> filter(
            @PageableDefault Pageable pageable,
            @Valid @RequestBody(required = false) FilterVacancyModerationReq filter
    ) {
        return ResponseEntity.ok(vacancyModerationAdminService.filter(pageable, filter));
    }

    @Operation(summary = "Карточка вакансии для модерации")
    @GetMapping("/{id}")
    public ResponseEntity<VacancyDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(vacancyModerationAdminService.getById(id));
    }

    @Operation(summary = "Одобрить вакансию (PUBLISHED)")
    @PostMapping("/{id}/approve")
    public ResponseEntity<VacancyDTO> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(vacancyModerationAdminService.approve(id));
    }

    @Operation(summary = "Отклонить вакансию")
    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(
            @PathVariable UUID id,
            @RequestBody(required = false) VacancyModerationRejectReq body
    ) {
        vacancyModerationAdminService.reject(id, body);
    }

    @Operation(summary = "Изменить порядок вакансий на витрине")
    @PostMapping("/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@Valid @RequestBody ReorderVacanciesReq req) {
        vacancyModerationAdminService.reorder(req);
    }

    @Operation(summary = "Настройки витрины вакансии (consent, sort)")
    @PatchMapping("/{id}/vitrina")
    public ResponseEntity<VacancyDTO> patchVitrina(
            @PathVariable UUID id,
            @Valid @RequestBody PatchVacancyVitrinaReq req
    ) {
        return ResponseEntity.ok(vacancyModerationAdminService.patchVitrina(id, req));
    }
}
