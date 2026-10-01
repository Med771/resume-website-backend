package ru.ai.sin.logic.request;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;

import ru.ai.sin.models.PageResponse;

import ru.ai.sin.logic.request.dto.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/request")
@Tag(name = "Request", description = "Операции управления заявками")
public class RequestController {

    private final RequestService requestService;
    private final TuDecisionService tuDecisionService;

    @Operation(summary = "Получить заявку по ID", description = "Возвращает детальную информацию о заявке")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(path = "/{id}")
    public ResponseEntity<RequestDTO> getById(@PathVariable @Min(1) long id) {
        return ResponseEntity.ok(requestService.getById(id));
    }

    @Operation(summary = "Фильтр заявок", description = "Принимает DTO фильтра в request body и Pageable без параметра sort")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(path = "/filter")
    public ResponseEntity<PageResponse<RequestDTO>> getByFilter(
            @PageableDefault Pageable pageable,
            @Valid @RequestBody FilterRequestReq filterRequestReq) {
        return ResponseEntity.ok(requestService.getByFilter(pageable, filterRequestReq));
    }

    @Operation(summary = "Мои заявки", description = "STUDENT или RECRUITER — только свои заявки")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER')")
    @PostMapping(path = "/mine/filter")
    public ResponseEntity<PageResponse<RequestDTO>> getMineByFilter(
            @PageableDefault Pageable pageable,
            @Valid @RequestBody(required = false) FilterRequestReq filterRequestReq) {
        return ResponseEntity.ok(requestService.getMineByFilter(pageable, filterRequestReq));
    }

    @Operation(
            summary = "Создать заявку",
            description = "Заявка от рекрутера на студента. Роль STUDENT создавать заявки не может. "
                    + "Студент с catalogVisible=false недоступен не-админу (ответ 404, как при отсутствии id). "
                    + "После первой заявки с полными данными профиль рекрутера привязывается к пользователю; "
                    + "далее достаточно studentId (проверка: GET /recruiter/me). В чате появляется системное сообщение об отправке.")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @PostMapping()
    public ResponseEntity<RequestDTO> create(@Valid @RequestBody AddRequestReq addRequestReq) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requestService.create(addRequestReq));
    }

    @Operation(summary = "Решение студента по заявке", description = "Принять или отклонить заявку; доступно только владельцу студента (роль STUDENT)")
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping(path = "/{id}/student-decision")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void studentDecision(
            @PathVariable @Min(1) long id,
            @Valid @RequestBody StudentRequestDecisionReq req) {
        requestService.studentRespond(id, req);
    }

    @Operation(summary = "Решение по ТУ", description = "Подтверждение или отказ по заявке на этапе ТУ")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER')")
    @PostMapping(path = "/{id}/tu-decision")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void tuDecision(
            @PathVariable @Min(1) long id,
            @Valid @RequestBody TuDecisionReq req) {
        tuDecisionService.decideOnRequest(id, req);
    }

    @Operation(summary = "Удалить заявку", description = "Удаляет заявку по ID")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable @Min(1) long id) {
        requestService.deleteById(id);
    }
}
