package ru.ai.sin.logic.account;

import ru.ai.sin.logic.account.dto.AccountApprovalUserDTO;
import ru.ai.sin.logic.account.dto.AccountRejectReq;
import ru.ai.sin.models.PageResponse;
import ru.ai.sin.models.enums.RoleEnum;

import java.util.UUID;

public interface AccountApprovalService {

    PageResponse<AccountApprovalUserDTO> listPending(RoleEnum role, int page, int size);

    void approve(UUID userId);

    void reject(UUID userId, AccountRejectReq body);
}
