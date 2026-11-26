package ru.ai.sin.logic.recruitment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.ai.sin.logic.recruitment.dto.FilterRecruitmentRequestReq;
import ru.ai.sin.logic.recruitment.dto.RecruitmentRequestDTO;
import ru.ai.sin.logic.recruitment.dto.RecruitmentResponseDTO;
import ru.ai.sin.logic.recruitment.dto.UpsertRecruitmentRequestReq;
import ru.ai.sin.models.PageResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/recruitment-requests")
@RequiredArgsConstructor
@Tag(name = "Recruitment requests", description = "Заявки работодателя на набор студентов. Старые /request, чат и вакансии не заменяет в контракте, они остаются.")
public class RecruitmentRequestController {

    private final RecruitmentRequestService recruitmentRequestService;

    @Operation(summary = "Создать заявку", description = "Работодатель только со статусом APPROVED. Админ создаёт свою заявку и передаёт companyName.")
    @PostMapping
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<RecruitmentRequestDTO> create(@Valid @RequestBody UpsertRecruitmentRequestReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recruitmentRequestService.create(req));
    }

    @Operation(summary = "Список заявок", description = "Админ видит все. Работодатель — только свои. Студент с одобренной анкетой — открытые внутри окна публикации.")
    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<PageResponse<RecruitmentRequestDTO>> filter(
            @PageableDefault Pageable pageable,
            @RequestBody(required = false) FilterRecruitmentRequestReq req) {
        return ResponseEntity.ok(recruitmentRequestService.filter(pageable, req));
    }

    @Operation(summary = "Заявка по id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<RecruitmentRequestDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(recruitmentRequestService.getById(id));
    }

    @Operation(summary = "Обновить заявку", description = "Владелец и админ. Архивную менять нельзя.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<RecruitmentRequestDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpsertRecruitmentRequestReq req) {
        return ResponseEntity.ok(recruitmentRequestService.update(id, req));
    }

    @Operation(summary = "Закрыть заявку", description = "Новые отклики больше не принимаются. Уже принятые отклики остаются.")
    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@PathVariable UUID id) {
        recruitmentRequestService.close(id);
    }

    @Operation(summary = "Отправить заявку в архив")
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id) {
        recruitmentRequestService.archive(id);
    }

    @Operation(summary = "Откликнуться", description = "Один раз. Нужна хотя бы одна одобренная версия анкеты.")
    @PostMapping("/{id}/responses")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<RecruitmentResponseDTO> respond(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recruitmentRequestService.respond(id));
    }

    @Operation(summary = "Отклики заявки", description = "Владелец и админ видят все. Студент — только свой.")
    @GetMapping("/{id}/responses")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<List<RecruitmentResponseDTO>> listResponses(@PathVariable UUID id) {
        return ResponseEntity.ok(recruitmentRequestService.listResponses(id));
    }

    @Operation(summary = "Принять отклик", description = "Заявку не закрывает, даже если набрано нужное число студентов.")
    @PostMapping("/{id}/responses/{responseId}/accept")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<RecruitmentResponseDTO> accept(@PathVariable UUID id, @PathVariable UUID responseId) {
        return ResponseEntity.ok(recruitmentRequestService.accept(id, responseId));
    }

    @Operation(summary = "Отклонить отклик")
    @PostMapping("/{id}/responses/{responseId}/reject")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<RecruitmentResponseDTO> reject(@PathVariable UUID id, @PathVariable UUID responseId) {
        return ResponseEntity.ok(recruitmentRequestService.reject(id, responseId));
    }
}
