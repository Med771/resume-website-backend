package ru.ai.sin.logic.account;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.account.dto.AccountRejectReq;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.student.StudentRepo;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.logic.verification.VerificationOtpMailer;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.RoleEnum;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock
    private VerificationOtpMailer verificationOtpMailer;

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
        user.setRegistrationEmail("student@test.local");

        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));
        when(securityHelper.getCurrentUsername()).thenReturn("admin");

        service.approve(USER_ID);

        ArgumentCaptor<StudentEnt> captor = ArgumentCaptor.forClass(StudentEnt.class);
        verify(studentRepo).save(captor.capture());
        assertThat(captor.getValue().isCatalogVisible()).isTrue();
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.APPROVED);
        verify(verificationOtpMailer).trySendAccountApproved("student@test.local", "student");
    }

    @Test
    void approve_recruiter_doesNotSendStudentEmail() {
        UserEnt user = new UserEnt(RoleEnum.RECRUITER, "r", "recruiter", "hash");
        user.setId(USER_ID);
        user.setAccountStatus(AccountStatus.PENDING_APPROVAL);

        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));
        when(securityHelper.getCurrentUsername()).thenReturn("admin");

        service.approve(USER_ID);

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.APPROVED);
        verify(verificationOtpMailer, never()).trySendAccountApproved(anyString(), anyString());
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

    @Test
    void approve_clearsStudentRejection() {
        StudentEnt student = new StudentEnt();
        student.setCatalogVisible(false);
        student.setRejectionReason("DUPLICATE");
        student.setRejectionComment("повтор");

        UserEnt user = new UserEnt(RoleEnum.STUDENT, "s", "student", "hash");
        user.setId(USER_ID);
        user.setAccountStatus(AccountStatus.PENDING_APPROVAL);
        user.setStudent(student);
        user.setEmailVerified(true);
        user.setRegistrationEmail("student@test.local");

        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));
        when(securityHelper.getCurrentUsername()).thenReturn("admin");

        service.approve(USER_ID);

        ArgumentCaptor<StudentEnt> captor = ArgumentCaptor.forClass(StudentEnt.class);
        verify(studentRepo).save(captor.capture());
        assertThat(captor.getValue().getRejectionReason()).isNull();
        assertThat(captor.getValue().getRejectionComment()).isNull();
        assertThat(captor.getValue().isCatalogVisible()).isTrue();
    }

    @Test
    void reject_student_savesReasonAndSendsEmail() {
        StudentEnt student = new StudentEnt();
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "s", "student", "hash");
        user.setId(USER_ID);
        user.setAccountStatus(AccountStatus.PENDING_APPROVAL);
        user.setStudent(student);
        user.setRegistrationEmail("student@test.local");

        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));
        when(securityHelper.getCurrentUsername()).thenReturn("admin");

        service.reject(USER_ID, new AccountRejectReq("incomplete_profile", "  Нет описания опыта  "));

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.REJECTED);
        assertThat(student.getRejectionReason()).isEqualTo("INCOMPLETE_PROFILE");
        assertThat(student.getRejectionComment()).isEqualTo("Нет описания опыта");
        verify(studentRepo).save(student);
        verify(verificationOtpMailer).trySendAccountRejected(
                eq("student@test.local"),
                eq("student"),
                eq("Анкета заполнена недостаточно"),
                eq("Нет описания опыта"));
    }

    @Test
    void reject_otherWithoutComment_throwsBadRequest() {
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "s", "student", "hash");
        user.setId(USER_ID);
        user.setAccountStatus(AccountStatus.PENDING_APPROVAL);
        user.setStudent(new StudentEnt());

        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.reject(USER_ID, new AccountRejectReq("OTHER", "  ")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("комментарий");
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.PENDING_APPROVAL);
        verify(userRepo, never()).save(user);
        verify(verificationOtpMailer, never()).trySendAccountRejected(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void reject_unknownReason_throwsBadRequest() {
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "s", "student", "hash");
        user.setId(USER_ID);
        user.setAccountStatus(AccountStatus.PENDING_APPROVAL);

        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.reject(USER_ID, new AccountRejectReq("NOPE", null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Неизвестная");
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.PENDING_APPROVAL);
        verify(userRepo, never()).save(user);
    }

    @Test
    void reject_recruiterWithoutBody_doesNotSendEmail() {
        UserEnt user = new UserEnt(RoleEnum.RECRUITER, "r", "recruiter", "hash");
        user.setId(USER_ID);
        user.setAccountStatus(AccountStatus.PENDING_APPROVAL);

        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));
        when(securityHelper.getCurrentUsername()).thenReturn("admin");

        service.reject(USER_ID, null);

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.REJECTED);
        verify(verificationOtpMailer, never()).trySendAccountRejected(anyString(), anyString(), anyString(), anyString());
        verify(studentRepo, never()).save(any(StudentEnt.class));
    }
}
