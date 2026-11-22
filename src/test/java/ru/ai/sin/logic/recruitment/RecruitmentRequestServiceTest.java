package ru.ai.sin.logic.recruitment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.exception.models.ConflictException;
import ru.ai.sin.exception.models.ForbiddenException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.logic.city.CityRepo;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.recruiter.RecruiterRepo;
import ru.ai.sin.logic.speciality.SpecialityRepo;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.student.StudentRepo;
import ru.ai.sin.logic.student.version.StudentProfileVersionService;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.RecruitmentRequestStatus;
import ru.ai.sin.models.enums.RecruitmentResponseStatus;
import ru.ai.sin.models.enums.RoleEnum;
import ru.ai.sin.tools.UserTools;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecruitmentRequestServiceTest {

    @Mock
    private RecruitmentRequestRepo requestRepo;
    @Mock
    private RecruitmentRequestSpecialityRepo specialityLinkRepo;
    @Mock
    private RecruitmentRequestCityRepo cityLinkRepo;
    @Mock
    private RecruitmentResponseRepo responseRepo;
    @Mock
    private SpecialityRepo specialityRepo;
    @Mock
    private CityRepo cityRepo;
    @Mock
    private RecruiterRepo recruiterRepo;
    @Mock
    private StudentRepo studentRepo;
    @Mock
    private UserTools userTools;
    @Mock
    private StudentProfileVersionService profileVersions;

    private RecruitmentRequestService service;

    private final UUID requestId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID recruiterId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private final UUID otherRecruiterId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private final UUID studentId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private final UUID responseId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    @BeforeEach
    void setUp() {
        service = new RecruitmentRequestService(
                requestRepo, specialityLinkRepo, cityLinkRepo, responseRepo,
                specialityRepo, cityRepo, recruiterRepo, studentRepo, userTools, profileVersions);
    }

    @Test
    void create_unapprovedRecruiter_forbidden() {
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(recruiter(AccountStatus.PENDING_APPROVAL, recruiterId)));

        assertThatThrownBy(() -> service.create(sampleReq()))
                .isInstanceOf(ForbiddenException.class);
        verify(requestRepo, never()).save(any());
    }

    @Test
    void getById_foreignRequest_notFoundForRecruiter() {
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(recruiter(AccountStatus.APPROVED, recruiterId)));
        RecruitmentRequestEnt request = openRequest();
        request.setRecruiterId(otherRecruiterId);
        when(requestRepo.findById(requestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.getById(requestId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getById_studentWithoutPublishedVersion_forbidden() {
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(student(AccountStatus.PENDING_APPROVAL)));
        when(requestRepo.findById(requestId)).thenReturn(Optional.of(openRequest()));
        when(profileVersions.hasPublishedVersion(studentId)).thenReturn(false);

        assertThatThrownBy(() -> service.getById(requestId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getById_studentInRevision_allowedWhenApprovedVersionExists() {
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(student(AccountStatus.IN_REVISION)));
        when(requestRepo.findById(requestId)).thenReturn(Optional.of(openRequest()));
        when(profileVersions.hasPublishedVersion(studentId)).thenReturn(true);

        assertThat(service.getById(requestId).status()).isEqualTo(RecruitmentRequestStatus.OPEN);
    }

    @Test
    void getById_outsidePublicationWindow_notFoundForStudent() {
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(student(AccountStatus.APPROVED)));
        RecruitmentRequestEnt request = openRequest();
        request.setPublishedTo(LocalDateTime.now().minusMinutes(1));
        when(requestRepo.findById(requestId)).thenReturn(Optional.of(request));
        when(profileVersions.hasPublishedVersion(studentId)).thenReturn(true);

        assertThatThrownBy(() -> service.getById(requestId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void respond_duplicate_conflict() {
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(student(AccountStatus.APPROVED)));
        when(requestRepo.findById(requestId)).thenReturn(Optional.of(openRequest()));
        when(profileVersions.hasPublishedVersion(studentId)).thenReturn(true);
        when(responseRepo.existsByRecruitmentRequestIdAndStudentId(requestId, studentId)).thenReturn(true);

        assertThatThrownBy(() -> service.respond(requestId))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void respond_closedRequest_notFound() {
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(student(AccountStatus.APPROVED)));
        RecruitmentRequestEnt request = openRequest();
        request.setStatus(RecruitmentRequestStatus.CLOSED);
        when(requestRepo.findById(requestId)).thenReturn(Optional.of(request));
        when(profileVersions.hasPublishedVersion(studentId)).thenReturn(true);

        assertThatThrownBy(() -> service.respond(requestId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void accept_doesNotCloseRequest() {
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(recruiter(AccountStatus.APPROVED, recruiterId)));
        RecruitmentRequestEnt request = openRequest();
        when(requestRepo.findById(requestId)).thenReturn(Optional.of(request));
        RecruitmentResponseEnt accepted = response(responseId);
        RecruitmentResponseEnt waiting = response(UUID.randomUUID());
        waiting.setStatus(RecruitmentResponseStatus.PENDING);
        when(responseRepo.findByIdAndRecruitmentRequestId(responseId, requestId)).thenReturn(Optional.of(accepted));
        when(responseRepo.save(accepted)).thenReturn(accepted);

        service.accept(requestId, responseId);

        assertThat(request.getStatus()).isEqualTo(RecruitmentRequestStatus.OPEN);
        assertThat(accepted.getStatus()).isEqualTo(RecruitmentResponseStatus.ACCEPTED);
        assertThat(waiting.getStatus()).isEqualTo(RecruitmentResponseStatus.PENDING);
        verify(requestRepo, never()).save(any());
    }

    private RecruitmentRequestEnt openRequest() {
        RecruitmentRequestEnt request = new RecruitmentRequestEnt();
        request.setId(requestId);
        request.setRecruiterId(recruiterId);
        request.setStatus(RecruitmentRequestStatus.OPEN);
        request.setTasks("задачи");
        request.setQualificationRequirements("требования");
        request.setWorkConditions("условия");
        request.setStudentCount(2);
        return request;
    }

    private RecruitmentResponseEnt response(UUID id) {
        RecruitmentResponseEnt response = new RecruitmentResponseEnt();
        response.setId(id);
        response.setRecruitmentRequestId(requestId);
        response.setStudentId(studentId);
        response.setStatus(RecruitmentResponseStatus.PENDING);
        return response;
    }

    private UserEnt recruiter(AccountStatus status, UUID id) {
        RecruiterEnt recruiter = new RecruiterEnt();
        recruiter.setId(id);
        UserEnt user = new UserEnt();
        user.setRole(RoleEnum.RECRUITER);
        user.setAccountStatus(status);
        user.setRecruiter(recruiter);
        return user;
    }

    private UserEnt student(AccountStatus status) {
        StudentEnt student = new StudentEnt();
        student.setId(studentId);
        UserEnt user = new UserEnt();
        user.setRole(RoleEnum.STUDENT);
        user.setAccountStatus(status);
        user.setStudent(student);
        return user;
    }

    private ru.ai.sin.logic.recruitment.dto.UpsertRecruitmentRequestReq sampleReq() {
        return new ru.ai.sin.logic.recruitment.dto.UpsertRecruitmentRequestReq(
                "задачи", "требования", "условия", 2, null, null, null, null, null, null,
                java.util.List.of(1L), java.util.List.of(1L), null);
    }
}
