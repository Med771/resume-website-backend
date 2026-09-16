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
            description = "Студенту — 400, если почта ещё не подтверждена (`emailVerified=false`). `PENDING_APPROVAL` при этом не меняется.")
    @PostMapping("/{userId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(@PathVariable UUID userId) {
        accountApprovalService.approve(userId);
    }

    @Operation(summary = "Отклонить аккаунт")
    @PostMapping("/{userId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable UUID userId, @Valid @RequestBody(required = false) AccountRejectReq body) {
        accountApprovalService.reject(userId, body);
    }

}
