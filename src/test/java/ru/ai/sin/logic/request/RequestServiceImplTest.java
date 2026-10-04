package ru.ai.sin.logic.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.logic.chat.ChatEnt;
import ru.ai.sin.logic.chat.ChatService;
import ru.ai.sin.logic.chat.ChatSystemEvent;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.request.dto.AddRequestReq;
import ru.ai.sin.logic.request.dto.RequestDTO;
import ru.ai.sin.logic.request.dto.StudentRequestDecisionReq;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.models.enums.CourseEnum;
import ru.ai.sin.models.enums.ResultEnum;
import ru.ai.sin.models.enums.RoleEnum;
import ru.ai.sin.logic.notification.UserInboxNotificationService;
import ru.ai.sin.models.enums.TuPhase;
import ru.ai.sin.tools.RecruiterTools;
import ru.ai.sin.tools.RequestTools;
import ru.ai.sin.tools.StudentTools;
import ru.ai.sin.tools.UserTools;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    @Mock
    private RequestRepo requestRepo;
    @Mock
    private RequestTools requestTools;
    @Mock
    private StudentTools studentTools;
    @Mock
    private RecruiterTools recruiterTools;
    @Mock
    private UserRepo userRepo;
    @Mock
    private UserTools userTools;
    @Mock
    private SecurityHelper securityHelper;
    @Mock
    private ChatService chatService;
    @Mock
    private AccountAccessHelper accountAccessHelper;
    @Mock
    private UserInboxNotificationService inboxNotificationService;

    private RequestServiceImpl service;

    private final UUID recruiterId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID studentId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID chatId = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() {
        service = new RequestServiceImpl(
                requestRepo,
                requestTools,
                studentTools,
                recruiterTools,
                userRepo,
                userTools,
                securityHelper,
                chatService,
                accountAccessHelper,
                inboxNotificationService
        );
    }

    private static RequestDTO sampleDto(
            long id, UUID chatId, ResultEnum result, UUID recruiterId, UUID studentId) {
        return new RequestDTO(
                id, chatId, result, null, null, null, recruiterId, studentId,
                null, null, null, null, null, null, TuPhase.NOT_APPLICABLE);
    }

    @Test
    void create_throwsWhenCurrentUserIsStudent() {
        UserEnt studentUser = new UserEnt();
        studentUser.setRole(RoleEnum.STUDENT);
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(studentUser));

        AddRequestReq req = new AddRequestReq(
                "ACME", "A", "B", "a@b.c", "+123", "tg", studentId
        );

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Студенты не могут отправлять заявки");

        verify(chatService, never()).getOrCreateChat(any(), any());
        verify(requestRepo, never()).save(any());
    }

    @Test
    void create_throwsNotFoundWhenStudentHiddenFromCatalogAndUserNotAdmin() {
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.empty());

        RecruiterEnt recruiter = new RecruiterEnt();
        recruiter.setId(recruiterId);
        when(recruiterTools.findOrCreateRecruiter(any())).thenReturn(recruiter);

        StudentEnt student = new StudentEnt();
        student.setId(studentId);
        student.setCatalogVisible(false);
        when(studentTools.getStudentOrThrow(studentId)).thenReturn(student);
        when(securityHelper.isCurrentUserAdmin()).thenReturn(false);

        AddRequestReq req = new AddRequestReq(
                "ACME", "A", "B", "a@b.c", "+123", "tg", studentId
        );

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Failed to find student");

        verify(chatService, never()).getOrCreateChat(any(), any());
        verify(requestRepo, never()).save(any());
    }

    @Test
    void create_allowsWhenStudentHiddenFromCatalogAndUserIsAdmin() {
        RecruiterEnt recruiter = new RecruiterEnt();
        recruiter.setId(recruiterId);
        UserEnt admin = new UserEnt(RoleEnum.ADMIN, "a", "admin", "x");
        admin.setRecruiter(recruiter);
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(admin));
        when(securityHelper.isCurrentUserAdmin()).thenReturn(true);

        StudentEnt student = new StudentEnt();
        student.setId(studentId);
        student.setCatalogVisible(false);
        when(studentTools.getStudentOrThrow(studentId)).thenReturn(student);

        ChatEnt chat = new ChatEnt();
        chat.setId(chatId);
        when(chatService.getOrCreateChat(recruiter, student)).thenReturn(chat);

        when(requestRepo.save(any(RequestEnt.class))).thenAnswer(inv -> {
            RequestEnt saved = inv.getArgument(0);
            saved.setId(200L);
            return saved;
        });

        RequestDTO dto = sampleDto(200L, chatId, ResultEnum.WAITING, recruiterId, studentId);
        when(requestTools.mapToDTO(any(RequestEnt.class))).thenReturn(dto);

        AddRequestReq req = new AddRequestReq(
                null, null, null, null, null, null, studentId
        );

        RequestDTO result = service.create(req);

        assertThat(result.id()).isEqualTo(200L);
        verify(chatService).getOrCreateChat(recruiter, student);
    }

    @Test
    void create_persistsRequestAndPostsRequestSent() {
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.empty());

        RecruiterEnt recruiter = new RecruiterEnt();
        recruiter.setId(recruiterId);
        when(recruiterTools.findOrCreateRecruiter(any())).thenReturn(recruiter);

        StudentEnt student = new StudentEnt();
        student.setId(studentId);
        when(studentTools.getStudentOrThrow(studentId)).thenReturn(student);

        ChatEnt chat = new ChatEnt();
        chat.setId(chatId);
        chat.setRecruiter(recruiter);
        chat.setStudent(student);
        when(chatService.getOrCreateChat(recruiter, student)).thenReturn(chat);

        when(requestRepo.save(any(RequestEnt.class))).thenAnswer(inv -> {
            RequestEnt saved = inv.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        RequestDTO dto = sampleDto(100L, chatId, ResultEnum.WAITING, recruiterId, studentId);
        when(requestTools.mapToDTO(any(RequestEnt.class))).thenReturn(dto);

        AddRequestReq req = new AddRequestReq(
                "ACME", "Ann", "Bee", "ann@acme.test", "+79990001122", "ann_tg", studentId
        );

        RequestDTO result = service.create(req);

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.appChatId()).isEqualTo(chatId);

        ArgumentCaptor<RequestEnt> captor = ArgumentCaptor.forClass(RequestEnt.class);
        verify(requestRepo).save(captor.capture());
        assertThat(captor.getValue().getResult()).isEqualTo(ResultEnum.WAITING);
        assertThat(captor.getValue().getAppChat()).isEqualTo(chat);

        verify(chatService).postSystemMessage(
                eq(chat),
                eq(ChatSystemEvent.REQUEST_SENT),
                eq("Заявка №100 отправлена. Ожидается решение студента.")
        );
    }

    @Test
    void studentRespond_acceptsAndPostsSystemMessage() {
        UUID userId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        StudentEnt student = new StudentEnt();
        student.setId(studentId);
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "s", "student", "x");
        user.setId(userId);
        user.setStudent(student);
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(user));

        ChatEnt chat = new ChatEnt();
        chat.setId(chatId);

        RequestEnt request = new RequestEnt();
        request.setId(7L);
        request.setResult(ResultEnum.WAITING);
        request.setStudent(student);
        request.setAppChat(chat);
        when(requestTools.getRequestOrThrow(7L)).thenReturn(request);

        service.studentRespond(7L, new StudentRequestDecisionReq(true, "ок"));

        verify(requestRepo).save(request);
        assertThat(request.getResult()).isEqualTo(ResultEnum.STUDENT_CONFIRMED);
        assertThat(request.getStudentResponseText()).isEqualTo("ок");
        verify(chatService).postSystemMessage(
                eq(chat),
                eq(ChatSystemEvent.STUDENT_ACCEPTED),
                eq("Студент принял заявку №7.")
        );
    }

    @Test
    void studentRespond_throwsWhenNotOwnerStudent() {
        StudentEnt other = new StudentEnt();
        other.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        StudentEnt mine = new StudentEnt();
        mine.setId(studentId);

        UserEnt user = new UserEnt(RoleEnum.STUDENT, "s", "student", "x");
        user.setStudent(mine);
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(user));

        RequestEnt request = new RequestEnt();
        request.setStudent(other);
        request.setAppChat(new ChatEnt());
        request.setResult(ResultEnum.WAITING);
        when(requestTools.getRequestOrThrow(1L)).thenReturn(request);

        assertThatThrownBy(() -> service.studentRespond(1L, new StudentRequestDecisionReq(true, null)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("не ваша заявка");
    }

    @Test
    void studentRespond_throwsWhenAlreadyDecided() {
        StudentEnt student = new StudentEnt();
        student.setId(studentId);
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "s", "student", "x");
        user.setStudent(student);
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(user));

        RequestEnt request = new RequestEnt();
        request.setStudent(student);
        request.setAppChat(new ChatEnt());
        request.setResult(ResultEnum.REFUSAL);
        when(requestTools.getRequestOrThrow(2L)).thenReturn(request);

        assertThatThrownBy(() -> service.studentRespond(2L, new StudentRequestDecisionReq(true, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("уже принято решение");
    }

    @Test
    void create_requiresCompanyNameWhenUserHasNoLinkedRecruiter() {
        UserEnt user = new UserEnt(RoleEnum.RECRUITER, "U", "guest1", "h");
        user.setRecruiter(null);
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(user));

        AddRequestReq req = new AddRequestReq(
                null, null, null, null, null, null, studentId
        );

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("companyName");

        verify(recruiterTools, never()).findOrCreateRecruiter(any());
    }

    @Test
    void create_usesLinkedRecruiterWithoutCompanyFields() {
        RecruiterEnt recruiter = new RecruiterEnt();
        recruiter.setId(recruiterId);
        UserEnt user = new UserEnt(RoleEnum.RECRUITER, "U", "rec_user", "h");
        user.setRecruiter(recruiter);
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(user));

        StudentEnt student = new StudentEnt();
        student.setId(studentId);
        when(studentTools.getStudentOrThrow(studentId)).thenReturn(student);

        ChatEnt chat = new ChatEnt();
        chat.setId(chatId);
        chat.setRecruiter(recruiter);
        chat.setStudent(student);
        when(chatService.getOrCreateChat(recruiter, student)).thenReturn(chat);

        when(requestRepo.save(any(RequestEnt.class))).thenAnswer(inv -> {
            RequestEnt saved = inv.getArgument(0);
            saved.setId(55L);
            return saved;
        });

        when(requestTools.mapToDTO(any(RequestEnt.class))).thenReturn(
                sampleDto(55L, chatId, ResultEnum.WAITING, recruiterId, studentId)
        );

        AddRequestReq req = new AddRequestReq(
                null, null, null, null, null, null, studentId
        );

        service.create(req);

        verify(recruiterTools, never()).findOrCreateRecruiter(any());
        verify(chatService).getOrCreateChat(recruiter, student);
    }

    @Test
    void studentRespond_rejectPostsStudentRejected() {
        StudentEnt student = new StudentEnt();
        student.setId(studentId);
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "s", "student", "x");
        user.setStudent(student);
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(user));

        ChatEnt chat = new ChatEnt();
        chat.setId(chatId);

        RequestEnt request = new RequestEnt();
        request.setId(9L);
        request.setResult(ResultEnum.WAITING);
        request.setStudent(student);
        request.setAppChat(chat);
        when(requestTools.getRequestOrThrow(9L)).thenReturn(request);

        service.studentRespond(9L, new StudentRequestDecisionReq(false, "не интересно"));

        assertThat(request.getResult()).isEqualTo(ResultEnum.REFUSAL);
        verify(chatService).postSystemMessage(
                eq(chat),
                eq(ChatSystemEvent.STUDENT_REJECTED),
                eq("Студент отклонил заявку №9.")
        );
    }

    @Test
    void studentRespond_allowedWhenResultIsCreation() {
        StudentEnt student = new StudentEnt();
        student.setId(studentId);
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "s", "student", "x");
        user.setStudent(student);
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(user));

        ChatEnt chat = new ChatEnt();
        RequestEnt request = new RequestEnt();
        request.setId(3L);
        request.setResult(ResultEnum.CREATION);
        request.setStudent(student);
        request.setAppChat(chat);
        when(requestTools.getRequestOrThrow(3L)).thenReturn(request);

        service.studentRespond(3L, new StudentRequestDecisionReq(true, null));

        assertThat(request.getResult()).isEqualTo(ResultEnum.STUDENT_CONFIRMED);
    }
}
