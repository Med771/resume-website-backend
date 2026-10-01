package ru.ai.sin.logic.account;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.student.StudentRepo;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.RoleEnum;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountApprovalServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private UserRepo userRepo;
    @Mock
    private StudentRepo studentRepo;
    @Mock
    private SecurityHelper securityHelper;

    @InjectMocks
    private AccountApprovalServiceImpl service;

    @Test
    void approve_setsCatalogVisibleForLinkedStudent() {
        StudentEnt student = new StudentEnt();
        student.setId(UUID.randomUUID());
        student.setCatalogVisible(false);

        UserEnt user = new UserEnt(RoleEnum.STUDENT, "s", "student", "hash");
        user.setId(USER_ID);
        user.setAccountStatus(AccountStatus.PENDING_APPROVAL);
        user.setStudent(student);
        user.setEmailVerified(true);

        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));
        when(securityHelper.getCurrentUsername()).thenReturn("admin");

        service.approve(USER_ID);

        ArgumentCaptor<StudentEnt> captor = ArgumentCaptor.forClass(StudentEnt.class);
        verify(studentRepo).save(captor.capture());
        assertThat(captor.getValue().isCatalogVisible()).isTrue();
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.APPROVED);
    }

    @Test
    void approve_studentWithoutEmailVerified_throwsBadRequest() {
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "s", "student", "hash");
        user.setId(USER_ID);
        user.setAccountStatus(AccountStatus.PENDING_APPROVAL);
        user.setEmailVerified(false);

        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.approve(USER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("почту");
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.PENDING_APPROVAL);
        verify(userRepo, never()).save(user);
    }
}
