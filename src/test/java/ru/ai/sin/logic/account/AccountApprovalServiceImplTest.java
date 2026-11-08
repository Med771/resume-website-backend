package ru.ai.sin.logic.account;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.account.dto.AccountApprovalUserDTO;
import ru.ai.sin.logic.account.dto.AccountEmailVerifiedReq;
import ru.ai.sin.logic.account.dto.AccountRejectReq;
import ru.ai.sin.logic.analytics.AnalyticsEventWriter;
import ru.ai.sin.logic.experience.ExperienceEnt;
import ru.ai.sin.logic.experience.ExperienceRepo;
import ru.ai.sin.logic.institution.InstitutionEnt;
import ru.ai.sin.logic.institution.InstitutionRepo;
import ru.ai.sin.logic.portfolio.PortfolioEnt;
import ru.ai.sin.logic.portfolio.PortfolioRepo;
import ru.ai.sin.logic.skill.SkillEnt;
import ru.ai.sin.logic.speciality.SpecialityEnt;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.student.StudentRepo;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.logic.verification.VerificationOtpMailer;
import ru.ai.sin.models.embeddables.UserInformation;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.BusynessEnum;
import ru.ai.sin.models.enums.CourseEnum;
import ru.ai.sin.models.enums.RoleEnum;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    private PortfolioRepo portfolioRepo;
    @Mock
    private ExperienceRepo experienceRepo;
    @Mock
    private InstitutionRepo institutionRepo;
    @Mock
    private SecurityHelper securityHelper;
    @Mock
    private VerificationOtpMailer verificationOtpMailer;
    @Mock
    private AnalyticsEventWriter analyticsEventWriter;

    @InjectMocks
    private AccountApprovalServiceImpl service;

    private static UserEnt studentUser(AccountStatus status) {
        StudentEnt student = new StudentEnt();
        student.setId(UUID.randomUUID());
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "s", "student", "hash");
        user.setAccountStatus(status);
        user.setStudent(student);
        return user;
    }

    @Test
    void approve_setsCatalogVisibleForLinkedStudent() {
        StudentEnt student = completeStudent();
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

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(analyticsEventWriter).record(
                eq("ACCOUNT_APPROVED"), eq("/admin/account-approvals/approve"), isNull(), payload.capture());
        assertThat(payload.getValue()).containsEntry("targetUserId", USER_ID);
        assertThat(payload.getValue()).containsEntry("role", "STUDENT");
    }

    @Test
    void listStudents_returnsEveryAccountStatus() {
        when(userRepo.findByRole(eq(RoleEnum.STUDENT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        studentUser(AccountStatus.PENDING_APPROVAL),
                        studentUser(AccountStatus.APPROVED),
                        studentUser(AccountStatus.REJECTED)
                )));
        when(portfolioRepo.findStudentIdsByStudentIdIn(any())).thenReturn(Set.of());
        when(experienceRepo.findStudentIdsByStudentIdIn(any())).thenReturn(Set.of());
        when(institutionRepo.findStudentIdsByStudentIdIn(any())).thenReturn(Set.of());

        var page = service.listStudents(0, 20);

        assertThat(page.data()).extracting(AccountApprovalUserDTO::accountStatus)
                .containsExactly(
                        AccountStatus.PENDING_APPROVAL,
                        AccountStatus.APPROVED,
                        AccountStatus.REJECTED);
        assertThat(page.data()).allSatisfy(dto -> {
            assertThat(dto.hasPortfolio()).isFalse();
            assertThat(dto.hasExperience()).isFalse();
            assertThat(dto.hasEducation()).isFalse();
        });
        verify(userRepo).findByRole(eq(RoleEnum.STUDENT), any(Pageable.class));
        verify(userRepo, never()).findByAccountStatus(any(), any());
    }

    @Test
    void listStudents_portfolioFlagOnly() {
        UUID studentId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        StudentEnt student = new StudentEnt();
        student.setId(studentId);
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "s", "student", "hash");
        user.setAccountStatus(AccountStatus.PENDING_APPROVAL);
        user.setStudent(student);

        when(userRepo.findByRole(eq(RoleEnum.STUDENT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(portfolioRepo.findStudentIdsByStudentIdIn(Set.of(studentId))).thenReturn(Set.of(studentId));
        when(experienceRepo.findStudentIdsByStudentIdIn(Set.of(studentId))).thenReturn(Set.of());
        when(institutionRepo.findStudentIdsByStudentIdIn(Set.of(studentId))).thenReturn(Set.of());

        var page = service.listStudents(0, 20);

        AccountApprovalUserDTO dto = page.data().getFirst();
        assertThat(dto.hasPortfolio()).isTrue();
        assertThat(dto.hasExperience()).isFalse();
        assertThat(dto.hasEducation()).isFalse();
    }

    @Test
    void listStudents_emptyPageDoesNotQuerySections() {
        when(userRepo.findByRole(eq(RoleEnum.STUDENT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var page = service.listStudents(0, 20);

        assertThat(page.data()).isEmpty();
        verify(portfolioRepo, never()).findStudentIdsByStudentIdIn(any());
        verify(experienceRepo, never()).findStudentIdsByStudentIdIn(any());
        verify(institutionRepo, never()).findStudentIdsByStudentIdIn(any());
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
    void approve_incompleteStudent_listsProblemsAndDoesNotSave() {
        StudentEnt student = new StudentEnt();
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "s", "student", "hash");
        user.setId(USER_ID);
        user.setAccountStatus(AccountStatus.PENDING_APPROVAL);
        user.setStudent(student);
        user.setEmailVerified(true);

        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.approve(USER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("нет фото")
                .hasMessageContaining("навыков меньше 5")
                .hasMessageContaining("описание короче 30 символов");
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.PENDING_APPROVAL);
        verify(userRepo, never()).save(any());
        verify(studentRepo, never()).save(any());
    }

    @Test
    void approve_studentWithoutProfile_throwsBadRequest() {
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "s", "student", "hash");
        user.setId(USER_ID);
        user.setAccountStatus(AccountStatus.PENDING_APPROVAL);
        user.setEmailVerified(true);

        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.approve(USER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("нет анкеты");
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.PENDING_APPROVAL);
        verify(userRepo, never()).save(any());
    }

    @Test
    void approve_clearsStudentRejection() {
        StudentEnt student = completeStudent();
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
        assertThat(student.isCatalogVisible()).isFalse();
        verify(studentRepo).save(student);
        verify(verificationOtpMailer).trySendAccountRejected(
                eq("student@test.local"),
                eq("student"),
                eq("Анкета заполнена недостаточно"),
                eq("Нет описания опыта"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(analyticsEventWriter).record(
                eq("ACCOUNT_REJECTED"), eq("/admin/account-approvals/reject"), isNull(), payload.capture());
        assertThat(payload.getValue()).containsEntry("reasonCode", "INCOMPLETE_PROFILE");
    }

    @Test
    void reject_approvedStudent_hidesCardAndSendsEmail() {
        StudentEnt student = new StudentEnt();
        student.setCatalogVisible(true);
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "s", "student", "hash");
        user.setId(USER_ID);
        user.setAccountStatus(AccountStatus.APPROVED);
        user.setStudent(student);
        user.setRegistrationEmail("student@test.local");

        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));
        when(securityHelper.getCurrentUsername()).thenReturn("admin");

        service.reject(USER_ID, new AccountRejectReq("INVALID_DATA", null));

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.REJECTED);
        assertThat(student.isCatalogVisible()).isFalse();
        assertThat(student.getRejectionReason()).isEqualTo("INVALID_DATA");
        verify(studentRepo).save(student);
        verify(verificationOtpMailer).trySendAccountRejected(
                eq("student@test.local"),
                eq("student"),
                eq("Данные или контакты выглядят некорректно"),
                isNull());
    }

    @Test
    void reject_approvedRecruiter_throwsBadRequest() {
        UserEnt user = new UserEnt(RoleEnum.RECRUITER, "r", "recruiter", "hash");
        user.setId(USER_ID);
        user.setAccountStatus(AccountStatus.APPROVED);

        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.reject(USER_ID, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("уже обработан");
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.APPROVED);
        verify(userRepo, never()).save(any());
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

    @Test
    void setEmailVerified_true_setsFlagAndClearsOtp() {
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "s", "student", "hash");
        user.setId(USER_ID);
        user.setAccountStatus(AccountStatus.PENDING_APPROVAL);
        user.setEmailVerified(false);
        user.setEmailOtpHash("hash");
        user.setEmailOtpExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));
        when(securityHelper.getCurrentUsername()).thenReturn("admin");

        service.setEmailVerified(USER_ID, new AccountEmailVerifiedReq(true));

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getEmailOtpHash()).isNull();
        assertThat(user.getEmailOtpExpiresAt()).isNull();
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.PENDING_APPROVAL);
        verify(userRepo).save(user);
        verify(verificationOtpMailer, never()).trySendAccountApproved(anyString(), anyString());
    }

    @Test
    void setEmailVerified_false_clearsFlagAndOtp() {
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "s", "student", "hash");
        user.setId(USER_ID);
        user.setAccountStatus(AccountStatus.APPROVED);
        user.setEmailVerified(true);
        user.setEmailOtpHash("hash");
        user.setEmailOtpExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));
        when(securityHelper.getCurrentUsername()).thenReturn("admin");

        service.setEmailVerified(USER_ID, new AccountEmailVerifiedReq(false));

        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getEmailOtpHash()).isNull();
        assertThat(user.getEmailOtpExpiresAt()).isNull();
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.APPROVED);
        verify(userRepo).save(user);
    }

    @Test
    void setEmailVerified_recruiter_throwsBadRequest() {
        UserEnt user = new UserEnt(RoleEnum.RECRUITER, "r", "recruiter", "hash");
        user.setId(USER_ID);
        user.setAccountStatus(AccountStatus.PENDING_APPROVAL);
        user.setEmailVerified(false);
        user.setEmailOtpHash("hash");

        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.setEmailVerified(USER_ID, new AccountEmailVerifiedReq(true)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("студента");
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getEmailOtpHash()).isEqualTo("hash");
        verify(userRepo, never()).save(any());
    }

    private static StudentEnt completeStudent() {
        StudentEnt student = new StudentEnt();
        student.setImagePath("photo.jpg");
        student.setBio("Описание анкеты длиннее тридцати символов");
        student.setCity("Москва");
        student.setCourse(CourseEnum.FIRST);
        student.setBusyness(BusynessEnum.FREE);
        student.setSpeciality(new SpecialityEnt());
        UserInformation name = new UserInformation();
        name.setFirstName("Иван");
        name.setLastName("Иванов");
        student.setUserInformation(name);
        student.getPortfolio().add(new PortfolioEnt());
        student.getCompanies().add(new ExperienceEnt());
        student.getEducation().add(new InstitutionEnt());
        for (int i = 0; i < StudentApprovalCompleteness.MIN_SKILLS; i++) {
            student.getSkills().add(new SkillEnt("skill-" + i));
        }
        return student;
    }
}
