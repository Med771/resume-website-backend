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
import ru.ai.sin.models.PageResponse;
import ru.ai.sin.models.enums.RoleEnum;

import java.util.UUID;

@RestController
@RequestMapping("/admin/account-approvals")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "AccountApprovals", description = "Одобрение аккаунтов студентов и рекрутеров")
public class AccountApprovalAdminController {

    private final AccountApprovalService accountApprovalService;

    @GetMapping
    public ResponseEntity<PageResponse<AccountApprovalUserDTO>> list(
            @RequestParam(required = false) RoleEnum role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(accountApprovalService.listPending(role, page, size));
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
                    Студенту нужен `reasonCode`: INCOMPLETE_PROFILE, INVALID_DATA, NOT_ELIGIBLE, DUPLICATE, OTHER.
                    Для OTHER обязателен `comment`. Студенту уходит письмо с причиной.
                    Рекрутеру тело можно не передавать.""")
    @PostMapping("/{userId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable UUID userId, @Valid @RequestBody(required = false) AccountRejectReq body) {
        accountApprovalService.reject(userId, body);
    }

}
