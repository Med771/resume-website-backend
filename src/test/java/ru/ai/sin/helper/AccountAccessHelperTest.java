package ru.ai.sin.helper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.ForbiddenException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.RoleEnum;
import ru.ai.sin.tools.StudentTools;
import ru.ai.sin.tools.UserTools;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountAccessHelperTest {

    private static final UUID OWN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private SecurityHelper securityHelper;
    @Mock
    private UserRepo userRepo;
    @Mock
    private UserTools userTools;
    @Mock
    private StudentTools studentTools;

    private AccountAccessHelper helper;

    @BeforeEach
    void setUp() {
        helper = new AccountAccessHelper(securityHelper, userRepo, userTools, studentTools);
    }

    @Test
    void requireStudentCanMutateResume_adminOk() {
        when(securityHelper.getCurrentUsername()).thenReturn("admin");
        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(user(RoleEnum.ADMIN, null)));

        assertThatCode(() -> helper.requireStudentCanMutateResume(OTHER_ID)).doesNotThrowAnyException();
    }

    @Test
    void requireStudentCanMutateResume_studentOwnOk() {
        when(securityHelper.getCurrentUsername()).thenReturn("stu");
        when(userRepo.findByUsername("stu")).thenReturn(Optional.of(user(RoleEnum.STUDENT, student(OWN_ID, true))));

        assertThatCode(() -> helper.requireStudentCanMutateResume(OWN_ID)).doesNotThrowAnyException();
    }

    @Test
    void requireStudentCanMutateResume_studentOtherForbidden() {
        when(securityHelper.getCurrentUsername()).thenReturn("stu");
        when(userRepo.findByUsername("stu")).thenReturn(Optional.of(user(RoleEnum.STUDENT, student(OWN_ID, true))));

        assertThatThrownBy(() -> helper.requireStudentCanMutateResume(OTHER_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requireStudentCanMutateResume_recruiterForbidden() {
        when(securityHelper.getCurrentUsername()).thenReturn("rec");
        when(userRepo.findByUsername("rec")).thenReturn(Optional.of(user(RoleEnum.RECRUITER, null)));

        assertThatThrownBy(() -> helper.requireStudentCanMutateResume(OWN_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requireCurrentStudentId_returnsOwnCard() {
        when(userTools.findCurrentUserFetchingLinks())
                .thenReturn(Optional.of(user(RoleEnum.STUDENT, student(OWN_ID, false))));

        assertThat(helper.requireCurrentStudentId()).isEqualTo(OWN_ID);
    }

    @Test
    void requireCurrentStudentId_adminNeedsStudentIdInBody() {
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(user(RoleEnum.ADMIN, null)));

        assertThatThrownBy(() -> helper.requireCurrentStudentId())
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("studentId");
    }

    @Test
    void requireCurrentStudentId_recruiterForbidden() {
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(user(RoleEnum.RECRUITER, null)));

        assertThatThrownBy(() -> helper.requireCurrentStudentId())
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void resolveStudentIdForResumeMutation_studentIgnoresRequestedId() {
        when(userTools.findCurrentUserFetchingLinks())
                .thenReturn(Optional.of(user(RoleEnum.STUDENT, student(OWN_ID, false))));

        assertThat(helper.resolveStudentIdForResumeMutation(OTHER_ID)).isEqualTo(OWN_ID);
        assertThat(helper.resolveStudentIdForResumeMutation(null)).isEqualTo(OWN_ID);
    }

    @Test
    void resolveStudentIdForResumeMutation_adminRequiresId() {
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(user(RoleEnum.ADMIN, null)));

        assertThatThrownBy(() -> helper.resolveStudentIdForResumeMutation(null))
                .isInstanceOf(BadRequestException.class);
        assertThat(helper.resolveStudentIdForResumeMutation(OTHER_ID)).isEqualTo(OTHER_ID);
    }

    @Test
    void requireCanReadStudentResumeDetails_ownHiddenCardOk() {
        when(userTools.findCurrentUserFetchingLinks())
                .thenReturn(Optional.of(user(RoleEnum.STUDENT, student(OWN_ID, false))));
        when(studentTools.getStudentOrThrow(OWN_ID)).thenReturn(student(OWN_ID, false));

        assertThatCode(() -> helper.requireCanReadStudentResumeDetails(OWN_ID)).doesNotThrowAnyException();
    }

    @Test
    void requireCanReadStudentResumeDetails_otherHidden_notFound() {
        stubApprovedRecruiter();
        when(studentTools.getStudentOrThrow(OTHER_ID)).thenReturn(student(OTHER_ID, false));
        when(securityHelper.isCurrentUserAdmin()).thenReturn(false);

        assertThatThrownBy(() -> helper.requireCanReadStudentResumeDetails(OTHER_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void requireCanReadStudentResumeDetails_otherVisible_ok() {
        stubApprovedRecruiter();
        when(studentTools.getStudentOrThrow(OTHER_ID)).thenReturn(student(OTHER_ID, true));

        assertThatCode(() -> helper.requireCanReadStudentResumeDetails(OTHER_ID)).doesNotThrowAnyException();
    }

    private void stubApprovedRecruiter() {
        UserEnt recruiter = user(RoleEnum.RECRUITER, null);
        recruiter.setAccountStatus(AccountStatus.APPROVED);
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(recruiter));
        when(securityHelper.getCurrentUsername()).thenReturn("rec");
        when(userRepo.findByUsername("rec")).thenReturn(Optional.of(recruiter));
    }

    private static UserEnt user(RoleEnum role, StudentEnt student) {
        UserEnt user = new UserEnt();
        user.setRole(role);
        user.setAccountStatus(AccountStatus.APPROVED);
        user.setStudent(student);
        return user;
    }

    private static StudentEnt student(UUID id, boolean catalogVisible) {
        StudentEnt student = new StudentEnt();
        student.setId(id);
        student.setCatalogVisible(catalogVisible);
        return student;
    }
}
