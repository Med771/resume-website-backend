package ru.ai.sin.logic.student;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.ProfileIncompleteException;
import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.helper.FileHelper;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.account.StudentReviewState;
import ru.ai.sin.logic.city.CityCatalog;
import ru.ai.sin.logic.city.CityEnt;
import ru.ai.sin.logic.analytics.StudentProfileChangeRecorder;
import ru.ai.sin.logic.student.version.StudentProfileVersionService;
import ru.ai.sin.logic.chat.ChatRepo;
import ru.ai.sin.logic.experience.ExperienceRepo;
import ru.ai.sin.logic.institution.InstitutionRepo;
import ru.ai.sin.logic.portfolio.PortfolioRepo;
import ru.ai.sin.logic.profile.ProfileCommunicationGateService;
import ru.ai.sin.logic.profile.dto.CommunicationReadinessRes;
import ru.ai.sin.logic.registration.RegistrationPasswordPolicy;
import ru.ai.sin.logic.verification.VerificationOtpMailer;
import ru.ai.sin.logic.request.RequestRepo;
import ru.ai.sin.logic.skill.SkillRepo;
import ru.ai.sin.logic.student.dto.PatchStudentMeReq;
import ru.ai.sin.logic.student.dto.StudentDTO;
import ru.ai.sin.logic.skill.dto.SkillDTO;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.BusynessEnum;
import ru.ai.sin.models.enums.CourseEnum;
import ru.ai.sin.models.enums.JobSearchStatus;
import ru.ai.sin.models.enums.RoleEnum;
import ru.ai.sin.tools.SkillTools;
import ru.ai.sin.tools.SpecialityTools;
import ru.ai.sin.tools.StudentTools;
import ru.ai.sin.tools.UserTools;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplPatchMeTest {

    private static final UUID ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private StudentRepo studentRepo;
    @Mock
    private SkillRepo skillRepo;
    @Mock
    private PortfolioRepo portfolioRepo;
    @Mock
    private ExperienceRepo experienceRepo;
    @Mock
    private InstitutionRepo institutionRepo;
    @Mock
    private RequestRepo requestRepo;
    @Mock
    private ChatRepo chatRepo;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private StudentTools studentTools;
    @Mock
    private SpecialityTools specialityTools;
    @Mock
    private SkillTools skillTools;
    @Mock
    private StudentSkillsMutator studentSkillsMutator;
    @Mock
    private FileHelper fileHelper;
    @Mock
    private SecurityHelper securityHelper;
    @Mock
    private AccountAccessHelper accountAccessHelper;
    @Mock
    private UserTools userTools;
    @Mock
    private UserRepo userRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RegistrationPasswordPolicy registrationPasswordPolicy;
    @Mock
    private ProfileCommunicationGateService profileCommunicationGateService;
    @Mock
    private VerificationOtpMailer verificationOtpMailer;
    @Mock
    private StudentReviewState studentReviewState;
    @Mock
    private StudentProfileChangeRecorder studentProfileChangeRecorder;
    @Mock
    private StudentProfileVersionService profileVersions;
    @Mock
    private CityCatalog cityCatalog;

    @InjectMocks
    private StudentServiceImpl studentService;

    @Test
    void deletePhoto_removesFileAndClearsPath() throws Exception {
        StudentEnt student = new StudentEnt();
        student.setId(ID);
        student.setImagePath("avatar.jpg");
        when(studentTools.getStudentOrThrow(ID)).thenReturn(student);

        studentService.deletePhoto(ID);

        assertThat(student.getImagePath()).isNull();
        verify(fileHelper).deleteFile("avatar.jpg");
        verify(studentReviewState).onStudentSelfEdit(student);
    }

    @Test
    void deletePhoto_missingFileStillClearsPath() throws Exception {
        StudentEnt student = new StudentEnt();
        student.setId(ID);
        student.setImagePath("gone.jpg");
        when(studentTools.getStudentOrThrow(ID)).thenReturn(student);
        doThrow(new FileNotFoundException("gone.jpg")).when(fileHelper).deleteFile("gone.jpg");

        studentService.deletePhoto(ID);

        assertThat(student.getImagePath()).isNull();
    }

    @Test
    void deletePhoto_blankPathDoesNotTouchFile() throws Exception {
        StudentEnt student = new StudentEnt();
        student.setId(ID);
        when(studentTools.getStudentOrThrow(ID)).thenReturn(student);

        studentService.deletePhoto(ID);

        verify(fileHelper, never()).deleteFile(any());
    }

    @Test
    void patchMe_writesBioAndSkills() {
        StudentEnt studentEnt = new StudentEnt();
        studentEnt.setId(ID);
        UserEnt user = new UserEnt();
        user.setRole(RoleEnum.STUDENT);
        user.setStudent(studentEnt);

        when(securityHelper.getCurrentUsername()).thenReturn("stu");
        when(userRepo.findByUsernameFetchingLinks("stu")).thenReturn(Optional.of(user));

        StudentDTO dto = new StudentDTO(
                ID, "c", "h", LocalDate.of(1999, 5, 5), "new bio", null,
                CourseEnum.FIRST, BusynessEnum.FREE, null, null, "A", "B", null, null, null, null, null, 1L, "S",
                List.of(new SkillDTO(1L, "Java", null, null)),
                false,
                false,
                0,
                null,
                null
        );
        when(studentTools.mapToDTO(studentEnt)).thenReturn(dto);

        PatchStudentMeReq req = new PatchStudentMeReq(
                null, null, null, "new bio", null, null, null, null,
                null, null, null, null, null, null, null, null,
                List.of(1L), null, null, null);

        assertThat(studentService.patchMe(req).bio()).isEqualTo("new bio");
        verify(studentReviewState).onStudentSelfEdit(studentEnt);
        assertThat(studentEnt.getBio()).isEqualTo("new bio");
        verify(studentSkillsMutator).replaceSkills(studentEnt, List.of(1L));
        verify(accountAccessHelper).requireStudentCanMutateResume(ID);
        assertThat(studentEnt.getProfileTextScore()).isGreaterThan(0);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<String>> fields = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(studentProfileChangeRecorder).record(
                eq(ID), eq("/student/me"), eq("card"), eq("update"), isNull(), fields.capture());
        assertThat(fields.getValue()).containsExactly("bio", "skillsIds");
        assertThat(fields.getValue()).noneMatch(field -> field.contains("new bio"));
    }

    @Test
    void patchMe_preferencesDoNotSendProfileForReview() {
        StudentEnt studentEnt = new StudentEnt();
        studentEnt.setId(ID);
        UserEnt user = new UserEnt();
        user.setRole(RoleEnum.STUDENT);
        user.setAccountStatus(AccountStatus.APPROVED);
        user.setStudent(studentEnt);
        when(securityHelper.getCurrentUsername()).thenReturn("stu");
        when(userRepo.findByUsernameFetchingLinks("stu")).thenReturn(Optional.of(user));
        when(studentTools.mapToDTO(studentEnt)).thenReturn(sampleDto());

        PatchStudentMeReq req = new PatchStudentMeReq(
                null, null, null, null, null, BusynessEnum.FREE, BusynessEnum.PROJECT_WORK, JobSearchStatus.LOOKING,
                null, null, null, null, null, null, null, null,
                null, null, null, null);

        studentService.patchMe(req);

        assertThat(studentEnt.getBusyness()).isEqualTo(BusynessEnum.FREE);
        assertThat(studentEnt.getDesiredBusyness()).isEqualTo(BusynessEnum.PROJECT_WORK);
        assertThat(studentEnt.getJobSearchStatus()).isEqualTo(JobSearchStatus.LOOKING);
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.APPROVED);
        verify(studentReviewState, never()).onStudentSelfEdit(any());
        verify(profileVersions, never()).captureCardPatch(any(), any());
        verify(profileVersions).patchLivePreferences(ID, req);
    }

    @Test
    void completeRegistration_withoutVerifiedEmail_doesNotSend() {
        StudentEnt studentEnt = new StudentEnt();
        studentEnt.setId(ID);
        UserEnt user = pendingStudent(studentEnt);
        user.setEmailVerified(false);
        when(securityHelper.getCurrentUsername()).thenReturn("stu");
        when(userRepo.findByUsernameFetchingLinks("stu")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> studentService.completeRegistration())
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("почту");
        verify(verificationOtpMailer, never()).trySendRegistrationSuccess("student@test.local", "stu");
    }

    @Test
    void completeRegistration_verified_sendsMail() {
        StudentEnt studentEnt = new StudentEnt();
        studentEnt.setId(ID);
        UserEnt user = pendingStudent(studentEnt);
        when(securityHelper.getCurrentUsername()).thenReturn("stu");
        when(userRepo.findByUsernameFetchingLinks("stu")).thenReturn(Optional.of(user));

        studentService.completeRegistration();

        verify(verificationOtpMailer).trySendRegistrationSuccess("student@test.local", "stu");
    }

    @Test
    void submitForReview_incomplete_doesNotSend() {
        StudentEnt studentEnt = new StudentEnt();
        studentEnt.setId(ID);
        UserEnt user = pendingStudent(studentEnt);
        when(securityHelper.getCurrentUsername()).thenReturn("stu");
        when(userRepo.findByUsernameFetchingLinks("stu")).thenReturn(Optional.of(user));
        when(profileCommunicationGateService.evaluateForCurrentUser())
                .thenReturn(new CommunicationReadinessRes(false, List.of("photo")));

        assertThatThrownBy(() -> studentService.submitForReview())
                .isInstanceOf(ProfileIncompleteException.class);
        verify(verificationOtpMailer, never()).trySendProfileSubmitted("student@test.local");
    }

    @Test
    void submitForReview_complete_sendsMail() {
        StudentEnt studentEnt = new StudentEnt();
        studentEnt.setId(ID);
        UserEnt user = pendingStudent(studentEnt);
        when(securityHelper.getCurrentUsername()).thenReturn("stu");
        when(userRepo.findByUsernameFetchingLinks("stu")).thenReturn(Optional.of(user));
        when(profileCommunicationGateService.evaluateForCurrentUser())
                .thenReturn(new CommunicationReadinessRes(true, List.of()));

        studentService.submitForReview();

        verify(verificationOtpMailer).trySendProfileSubmitted("student@test.local");
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.PENDING_APPROVAL);
    }

    @Test
    void submitForReview_registration_movesToPending() {
        StudentEnt studentEnt = new StudentEnt();
        studentEnt.setId(ID);
        UserEnt user = pendingStudent(studentEnt);
        user.setAccountStatus(AccountStatus.REGISTRATION);
        when(securityHelper.getCurrentUsername()).thenReturn("stu");
        when(userRepo.findByUsernameFetchingLinks("stu")).thenReturn(Optional.of(user));
        when(profileCommunicationGateService.evaluateForCurrentUser())
                .thenReturn(new CommunicationReadinessRes(true, List.of()));

        studentService.submitForReview();

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.PENDING_APPROVAL);
        verify(verificationOtpMailer).trySendProfileSubmitted("student@test.local");
    }

    @Test
    void submitForReview_approvedWithoutDraft_isBadRequest() {
        StudentEnt studentEnt = new StudentEnt();
        studentEnt.setId(ID);
        UserEnt user = pendingStudent(studentEnt);
        user.setAccountStatus(AccountStatus.APPROVED);
        when(securityHelper.getCurrentUsername()).thenReturn("stu");
        when(userRepo.findByUsernameFetchingLinks("stu")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> studentService.submitForReview())
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("обработан");
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.APPROVED);
        verify(verificationOtpMailer, never()).trySendProfileSubmitted(any());
    }

    @Test
    void patchMe_cityText_usesCatalog() {
        StudentEnt studentEnt = new StudentEnt();
        studentEnt.setId(ID);
        UserEnt user = new UserEnt();
        user.setRole(RoleEnum.STUDENT);
        user.setStudent(studentEnt);
        when(securityHelper.getCurrentUsername()).thenReturn("stu");
        when(userRepo.findByUsernameFetchingLinks("stu")).thenReturn(Optional.of(user));
        when(studentTools.mapToDTO(studentEnt)).thenReturn(sampleDto());
        doAnswer(invocation -> {
            StudentEnt student = invocation.getArgument(0);
            student.setCity("Казань");
            return null;
        }).when(cityCatalog).assignIfPresent(any(), isNull(), eq("казань"));

        studentService.patchMe(cityPatch("казань"));

        assertThat(studentEnt.getCity()).isEqualTo("Казань");
    }

    @Test
    void patchMe_unknownCity_isBadRequest() {
        StudentEnt studentEnt = new StudentEnt();
        studentEnt.setId(ID);
        studentEnt.setCity("Казань");
        UserEnt user = new UserEnt();
        user.setRole(RoleEnum.STUDENT);
        user.setStudent(studentEnt);
        when(securityHelper.getCurrentUsername()).thenReturn("stu");
        when(userRepo.findByUsernameFetchingLinks("stu")).thenReturn(Optional.of(user));
        doThrow(new BadRequestException("Город не найден в справочнике"))
                .when(cityCatalog).assignIfPresent(any(), isNull(), eq("Нет такого"));

        assertThatThrownBy(() -> studentService.patchMe(cityPatch("Нет такого")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("справочник");
        assertThat(studentEnt.getCity()).isEqualTo("Казань");
    }

    @Test
    void patchMe_withoutCity_keepsChosenCity() {
        StudentEnt studentEnt = new StudentEnt();
        studentEnt.setId(ID);
        CityEnt city = new CityEnt();
        city.setId(5L);
        city.setName("Казань");
        studentEnt.setCity("Казань");
        studentEnt.setCityCatalog(city);
        UserEnt user = new UserEnt();
        user.setRole(RoleEnum.STUDENT);
        user.setStudent(studentEnt);
        when(securityHelper.getCurrentUsername()).thenReturn("stu");
        when(userRepo.findByUsernameFetchingLinks("stu")).thenReturn(Optional.of(user));
        when(studentTools.mapToDTO(studentEnt)).thenReturn(sampleDto());

        studentService.patchMe(cityPatch(null));

        assertThat(studentEnt.getCity()).isEqualTo("Казань");
        assertThat(studentEnt.getCityCatalog()).isSameAs(city);
    }

    @Test
    void patchMe_pendingWithPublishedVersion_movesToRevision() {
        UserEnt user = pendingEditor();
        when(profileVersions.captureCardPatch(eq(ID), any())).thenReturn(true);
        when(profileVersions.draftCard(user.getStudent())).thenReturn(Optional.of(sampleDto()));
        useRealReviewState();

        studentService.patchMe(bioPatch());

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.IN_REVISION);
    }

    @Test
    void patchMe_pendingWithoutPublishedVersion_movesToRevision() {
        UserEnt user = pendingEditor();
        when(studentTools.mapToDTO(user.getStudent())).thenReturn(sampleDto());
        useRealReviewState();

        studentService.patchMe(bioPatch());

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.IN_REVISION);
        assertThat(user.getStudent().getBio()).isEqualTo("new bio");
    }

    @Test
    void patchMe_approvedWithDraft_movesToRevision() {
        UserEnt user = pendingEditor();
        user.setAccountStatus(AccountStatus.APPROVED);
        user.getStudent().setCatalogVisible(true);
        when(profileVersions.captureCardPatch(eq(ID), any())).thenReturn(true);
        when(profileVersions.draftCard(user.getStudent())).thenReturn(Optional.of(sampleDto()));
        useRealReviewState();

        studentService.patchMe(bioPatch());

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.IN_REVISION);
        assertThat(user.getStudent().isCatalogVisible()).isFalse();
    }

    @Test
    void setPhoto_pendingWithDraft_movesToRevision() {
        UserEnt user = pendingEditor();
        when(studentTools.getStudentOrThrow(ID)).thenReturn(user.getStudent());
        MultipartFile file = mock(MultipartFile.class);
        when(fileHelper.saveFile(file, ID.toString())).thenReturn("photo.jpg");
        when(profileVersions.capturePhoto(ID, "photo.jpg")).thenReturn(true);
        useRealReviewState();

        studentService.setPhoto(ID, file);

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.IN_REVISION);
    }

    private UserEnt pendingEditor() {
        StudentEnt studentEnt = new StudentEnt();
        studentEnt.setId(ID);
        UserEnt user = new UserEnt();
        user.setRole(RoleEnum.STUDENT);
        user.setAccountStatus(AccountStatus.PENDING_APPROVAL);
        user.setStudent(studentEnt);
        lenient().when(securityHelper.getCurrentUsername()).thenReturn("stu");
        lenient().when(securityHelper.getCurrentRoleOptional()).thenReturn(Optional.of(RoleEnum.STUDENT.getRole()));
        lenient().when(userRepo.findByUsernameFetchingLinks("stu")).thenReturn(Optional.of(user));
        lenient().when(userRepo.findByStudent_Id(ID)).thenReturn(Optional.of(user));
        return user;
    }

    private void useRealReviewState() {
        StudentReviewState real = new StudentReviewState(userRepo, securityHelper);
        doAnswer(invocation -> {
            real.onStudentSelfEdit(invocation.getArgument(0));
            return null;
        }).when(studentReviewState).onStudentSelfEdit(any());
    }

    private static PatchStudentMeReq bioPatch() {
        return new PatchStudentMeReq(
                null, null, null, "new bio", null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null);
    }

    private static PatchStudentMeReq cityPatch(String city) {
        return new PatchStudentMeReq(
                city, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null);
    }

    private static StudentDTO sampleDto() {
        return new StudentDTO(
                ID, "c", "h", LocalDate.of(1999, 5, 5), "bio", null,
                CourseEnum.FIRST, BusynessEnum.FREE, null, null, "A", "B", null, null, null, null, null, 1L, "S",
                List.of(new SkillDTO(1L, "Java", null, null)),
                false, false, 0, null, null);
    }

    private static UserEnt pendingStudent(StudentEnt studentEnt) {
        UserEnt user = new UserEnt();
        user.setRole(RoleEnum.STUDENT);
        user.setUsername("stu");
        user.setStudent(studentEnt);
        user.setEmailVerified(true);
        user.setAccountStatus(AccountStatus.PENDING_APPROVAL);
        user.setRegistrationEmail("student@test.local");
        return user;
    }
}
