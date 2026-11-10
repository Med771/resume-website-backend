package ru.ai.sin.logic.account;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.ai.sin.logic.account.dto.AccountApprovalUserDTO;
import ru.ai.sin.logic.account.dto.AccountEmailVerifiedReq;
import ru.ai.sin.logic.account.dto.AccountRejectReq;
import ru.ai.sin.logic.account.dto.RecruiterApprovalSortField;
import ru.ai.sin.logic.student.dto.StudentSortDirection;
import ru.ai.sin.models.PageResponse;
import ru.ai.sin.models.enums.AccountStatus;

import java.util.UUID;

@RestController
@RequestMapping("/admin/account-approvals")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "AccountApprovals", description = "Одобрение аккаунтов студентов и рекрутеров")
public class AccountApprovalAdminController {

    private final AccountApprovalService accountApprovalService;

    @Operation(
            summary = "Все студенты со статусом аккаунта",
            description = """
                    Страница пользователей с ролью STUDENT: `PENDING_APPROVAL`, `APPROVED` и `REJECTED`.
                    В строке `id` пользователя и `studentId` карточки.
                    `hasPortfolio`, `hasExperience`, `hasEducation` — есть ли хотя бы одна запись.
                    Пагинация: `page`, `size`.""")
    @GetMapping("/students")
    public ResponseEntity<PageResponse<AccountApprovalUserDTO>> listStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(accountApprovalService.listStudents(page, size));
    }

    @Operation(
            summary = "Все работодатели со статусом аккаунта",
            description = """
                    Страница пользователей с ролью RECRUITER: `PENDING_APPROVAL`, `APPROVED` и `REJECTED`.
                    Необязательные фильтры: `findString`, `accountStatus`, `emailVerified`, `city`.
                    Сортировка: `sortBy` (`CREATED_AT`, `COMPANY_NAME`, `LAST_NAME`, `ACCOUNT_STATUS`) и `sortDirection`.
                    Без сортировки — новые сверху. Пагинация: `page`, `size`.""")
    @GetMapping("/recruiters")
    public ResponseEntity<PageResponse<AccountApprovalUserDTO>> listRecruiters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String findString,
            @RequestParam(required = false) AccountStatus accountStatus,
            @RequestParam(required = false) Boolean emailVerified,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) RecruiterApprovalSortField sortBy,
            @RequestParam(required = false) StudentSortDirection sortDirection
    ) {
        return ResponseEntity.ok(accountApprovalService.listRecruiters(
                page, size, findString, accountStatus, emailVerified, city, sortBy, sortDirection));
    }

    @Operation(
            summary = "Одобрить аккаунт",
            description = """
                    Студенту — 400, если почта ещё не подтверждена (`emailVerified=false`)
                    или анкета неполная. В `message` перечислены все недостающие или некорректные поля.
                    `PENDING_APPROVAL` при этом не меняется.""")
    @PostMapping("/{userId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(@PathVariable UUID userId) {
        accountApprovalService.approve(userId);
    }

    @Operation(
            summary = "Подтвердить или снять подтверждение почты студента",
            description = """
                    Временное ручное изменение `users.email_verified`. Только STUDENT.
                    Код подтверждения из письма сбрасывается. Статус аккаунта не меняется, письмо не отправляется.""")
    @PostMapping("/{userId}/email-verified")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setEmailVerified(@PathVariable UUID userId, @Valid @RequestBody AccountEmailVerifiedReq body) {
        accountApprovalService.setEmailVerified(userId, body);
    }

    @Operation(
            summary = "Отклонить аккаунт",
            description = """
                    Студента можно отклонить из `PENDING_APPROVAL` и из `APPROVED`.
                    Нужен `reasonCode`: INCOMPLETE_PROFILE, INVALID_DATA, NOT_ELIGIBLE, DUPLICATE, OTHER.
                    Для OTHER обязателен `comment`. Студенту уходит письмо с причиной, карточка скрывается.
                    Рекрутера — только из `PENDING_APPROVAL`, тело можно не передавать.""")
    @PostMapping("/{userId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable UUID userId, @Valid @RequestBody(required = false) AccountRejectReq body) {
        accountApprovalService.reject(userId, body);
    }

}
