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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.ai.sin.logic.request.dto.TuDecisionReq;
import ru.ai.sin.logic.vacancy.dto.ApplyVacancyReq;
import ru.ai.sin.logic.vacancy.dto.CreateVacancyReq;
import ru.ai.sin.logic.vacancy.dto.FilterVacancyReq;
import ru.ai.sin.logic.vacancy.dto.RejectApplicationReq;
import ru.ai.sin.logic.vacancy.dto.UpdateVacancyReq;
import ru.ai.sin.logic.vacancy.dto.VacancyApplicationDTO;
import ru.ai.sin.logic.vacancy.dto.VacancyCardDTO;
import ru.ai.sin.logic.vacancy.dto.VacancyDTO;
import ru.ai.sin.models.PageResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/vacancies")
@RequiredArgsConstructor
@Validated
@Tag(name = "Vacancies", description = "Вакансии рекрутёра и витрина для авторизованных")
public class VacancyController {

    private final VacancyService vacancyService;
    private final VacancyApplicationService vacancyApplicationService;
    private final VacancyApplicationTuDecisionService vacancyApplicationTuDecisionService;

    @Operation(summary = "Лента опубликованных вакансий")
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<PageResponse<VacancyCardDTO>> list(
            @PageableDefault Pageable pageable,
            @Valid @ModelAttribute FilterVacancyReq filter
    ) {
        return ResponseEntity.ok(vacancyService.listPublishedFeed(pageable, filter));
    }

    @Operation(summary = "Вакансии текущего рекрутёра")
    @GetMapping("/mine")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<List<VacancyDTO>> listMine() {
        return ResponseEntity.ok(vacancyService.listMine());
    }

    @Operation(summary = "Мои отклики")
    @GetMapping("/applications/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<PageResponse<VacancyApplicationDTO>> listMyApplications(
            @PageableDefault Pageable pageable
    ) {
        return ResponseEntity.ok(vacancyApplicationService.listMine(pageable));
    }

    @Operation(summary = "Деталь вакансии")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<VacancyDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(vacancyService.getById(id));
    }

    @Operation(summary = "Создать черновик вакансии")
    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    @ResponseStatus(HttpStatus.CREATED)
    public VacancyDTO create(@Valid @RequestBody CreateVacancyReq req) {
        return vacancyService.create(req);
    }

    @Operation(summary = "Обновить черновик или отклонённую вакансию")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<VacancyDTO> update(@PathVariable UUID id, @Valid @RequestBody UpdateVacancyReq req) {
        return ResponseEntity.ok(vacancyService.update(id, req));
    }

    @Operation(summary = "Отправить на модерацию")
    @PostMapping("/{id}/submit-for-review")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<VacancyDTO> submitForReview(@PathVariable UUID id) {
        return ResponseEntity.ok(vacancyService.submitForReview(id));
    }

    @Operation(summary = "Закрыть вакансию (прекратить отклики)")
    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<VacancyDTO> close(@PathVariable UUID id) {
        return ResponseEntity.ok(vacancyService.close(id));
    }

    @Operation(summary = "Архивировать вакансию (или удалить пустой черновик)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        vacancyService.delete(id);
    }

    @Operation(summary = "Откликнуться на вакансию")
    @PostMapping("/{id}/applications")
    @PreAuthorize("hasRole('STUDENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public VacancyApplicationDTO apply(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ApplyVacancyReq req
    ) {
        return vacancyApplicationService.apply(id, req);
    }

    @Operation(summary = "Отозвать отклик")
    @PostMapping("/applications/{applicationId}/withdraw")
    @PreAuthorize("hasRole('STUDENT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@PathVariable UUID applicationId) {
        vacancyApplicationService.withdraw(applicationId);
    }

    @Operation(summary = "Отклики на вакансию (рекрутер)")
    @GetMapping("/{id}/applications")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<PageResponse<VacancyApplicationDTO>> listApplications(
            @PathVariable UUID id,
            @PageableDefault Pageable pageable
    ) {
        return ResponseEntity.ok(vacancyApplicationService.listForVacancy(id, pageable));
    }

    @Operation(summary = "Принять отклик")
    @PostMapping("/{id}/applications/{applicationId}/accept")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<VacancyApplicationDTO> accept(
            @PathVariable UUID id,
            @PathVariable UUID applicationId
    ) {
        return ResponseEntity.ok(vacancyApplicationService.accept(id, applicationId));
    }

    @Operation(summary = "Отклонить отклик")
    @PostMapping("/{id}/applications/{applicationId}/reject")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<VacancyApplicationDTO> reject(
            @PathVariable UUID id,
            @PathVariable UUID applicationId,
            @RequestBody(required = false) RejectApplicationReq req
    ) {
        return ResponseEntity.ok(vacancyApplicationService.reject(id, applicationId, req));
    }

    @Operation(summary = "Решение по ТУ (отклик на вакансию)")
    @PostMapping("/applications/{applicationId}/tu-decision")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void tuDecision(
            @PathVariable UUID applicationId,
            @Valid @RequestBody TuDecisionReq req
    ) {
        vacancyApplicationTuDecisionService.decide(applicationId, req);
    }
}
