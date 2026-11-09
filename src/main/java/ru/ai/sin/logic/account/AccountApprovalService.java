package ru.ai.sin.logic.account;

import ru.ai.sin.logic.account.dto.AccountApprovalUserDTO;
import ru.ai.sin.logic.account.dto.AccountEmailVerifiedReq;
import ru.ai.sin.logic.account.dto.AccountRejectReq;
import ru.ai.sin.logic.account.dto.RecruiterApprovalSortField;
import ru.ai.sin.logic.student.dto.StudentSortDirection;
import ru.ai.sin.models.PageResponse;
import ru.ai.sin.models.enums.AccountStatus;

import java.util.UUID;

public interface AccountApprovalService {

    PageResponse<AccountApprovalUserDTO> listStudents(int page, int size);

    PageResponse<AccountApprovalUserDTO> listRecruiters(
            int page,
            int size,
            String findString,
            AccountStatus accountStatus,
            Boolean emailVerified,
            String city,
            RecruiterApprovalSortField sortBy,
            StudentSortDirection sortDirection
    );

    void approve(UUID userId);

    void setEmailVerified(UUID userId, AccountEmailVerifiedReq body);

    void reject(UUID userId, AccountRejectReq body);
}
