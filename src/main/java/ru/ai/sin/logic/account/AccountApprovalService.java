package ru.ai.sin.logic.account;

import ru.ai.sin.logic.account.dto.AccountApprovalUserDTO;
import ru.ai.sin.logic.account.dto.AccountEmailVerifiedReq;
import ru.ai.sin.logic.account.dto.AccountRejectReq;
import ru.ai.sin.models.PageResponse;

import java.util.UUID;

public interface AccountApprovalService {

    PageResponse<AccountApprovalUserDTO> listStudents(int page, int size);

    void approve(UUID userId);

    void setEmailVerified(UUID userId, AccountEmailVerifiedReq body);

    void reject(UUID userId, AccountRejectReq body);
}
