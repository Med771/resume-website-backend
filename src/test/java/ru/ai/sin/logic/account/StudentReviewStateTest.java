package ru.ai.sin.logic.account;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.RoleEnum;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentReviewStateTest {

    private static final UUID STUDENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private UserRepo userRepo;
    @Mock
    private SecurityHelper securityHelper;

    @InjectMocks
    private StudentReviewState reviewState;

    @Test
    void selfEdit_approved_returnsToReviewAndHidesCard() {
        StudentEnt student = student();
        student.setCatalogVisible(true);
        UserEnt user = user(AccountStatus.APPROVED);
        when(securityHelper.getCurrentRoleOptional()).thenReturn(Optional.of("STUDENT"));
        when(userRepo.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.of(user));

        reviewState.onStudentSelfEdit(student);

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.PENDING_APPROVAL);
        assertThat(student.isCatalogVisible()).isFalse();
        verify(userRepo).save(user);
    }

    @Test
    void selfEdit_rejected_returnsToReviewAndClearsReason() {
        StudentEnt student = student();
        student.setCatalogVisible(true);
        student.setRejectionReason("INCOMPLETE_PROFILE");
        student.setRejectionComment("Нет фото");
        UserEnt user = user(AccountStatus.REJECTED);
        when(securityHelper.getCurrentRoleOptional()).thenReturn(Optional.of("STUDENT"));
        when(userRepo.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.of(user));

        reviewState.onStudentSelfEdit(student);

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.PENDING_APPROVAL);
        assertThat(student.isCatalogVisible()).isFalse();
        assertThat(student.getRejectionReason()).isNull();
        assertThat(student.getRejectionComment()).isNull();
    }

    @Test
    void selfEdit_pending_doesNotChangeStatus() {
        StudentEnt student = student();
        UserEnt user = user(AccountStatus.PENDING_APPROVAL);
        when(securityHelper.getCurrentRoleOptional()).thenReturn(Optional.of("STUDENT"));
        when(userRepo.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.of(user));

        reviewState.onStudentSelfEdit(student);

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.PENDING_APPROVAL);
        verify(userRepo, never()).save(user);
    }

    @Test
    void selfEdit_admin_doesNotChangeStatus() {
        StudentEnt student = student();
        student.setCatalogVisible(true);
        when(securityHelper.getCurrentRoleOptional()).thenReturn(Optional.of("ADMIN"));

        reviewState.onStudentSelfEdit(student);

        assertThat(student.isCatalogVisible()).isTrue();
        verify(userRepo, never()).findByStudent_Id(STUDENT_ID);
    }

    @Test
    void catalogVisible_approved_canBeHidden() {
        assertThat(reviewState.catalogVisible(student(), false)).isFalse();
    }

    @Test
    void catalogVisible_notApproved_cannotBeShown() {
        UserEnt user = user(AccountStatus.PENDING_APPROVAL);
        when(userRepo.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.of(user));

        assertThat(reviewState.catalogVisible(student(), true)).isFalse();
    }

    @Test
    void catalogVisible_approved_canBeShown() {
        UserEnt user = user(AccountStatus.APPROVED);
        when(userRepo.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.of(user));

        assertThat(reviewState.catalogVisible(student(), true)).isTrue();
    }

    private static StudentEnt student() {
        StudentEnt student = new StudentEnt();
        student.setId(STUDENT_ID);
        return student;
    }

    private static UserEnt user(AccountStatus status) {
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "s", "student", "hash");
        user.setAccountStatus(status);
        return user;
    }
}
