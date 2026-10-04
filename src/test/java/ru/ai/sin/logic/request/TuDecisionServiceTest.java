package ru.ai.sin.logic.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.logic.chat.ChatEnt;
import ru.ai.sin.logic.chat.ChatService;
import ru.ai.sin.logic.notification.UserInboxNotificationService;
import ru.ai.sin.logic.request.dto.TuDecisionReq;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.models.enums.ResultEnum;
import ru.ai.sin.models.enums.RoleEnum;
import ru.ai.sin.tools.RequestTools;
import ru.ai.sin.tools.UserTools;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TuDecisionServiceTest {

    @Mock
    private RequestRepo requestRepo;
    @Mock
    private RequestTools requestTools;
    @Mock
    private UserTools userTools;
    @Mock
    private ChatService chatService;
    @Mock
    private AccountAccessHelper accountAccessHelper;
    @Mock
    private UserInboxNotificationService inboxNotificationService;

    @InjectMocks
    private TuDecisionService service;

    @Test
    void studentCannotConfirmTuFromWaiting() {
        RequestEnt request = buildRequest(ResultEnum.WAITING);
        UserEnt studentUser = studentUser(request.getStudent().getId());

        when(requestTools.getRequestOrThrow(1L)).thenReturn(request);
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(studentUser));

        assertThatThrownBy(() -> service.decideOnRequest(1L, new TuDecisionReq(true, null, null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void studentCanConfirmTuAfterStudentDecision() {
        RequestEnt request = buildRequest(ResultEnum.STUDENT_CONFIRMED);
        UserEnt studentUser = studentUser(request.getStudent().getId());

        when(requestTools.getRequestOrThrow(1L)).thenReturn(request);
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(studentUser));

        service.decideOnRequest(1L, new TuDecisionReq(true, null, null));

        verify(requestRepo).save(request);
        verify(chatService).postSystemMessage(
                eq(request.getAppChat()),
                eq(ru.ai.sin.logic.chat.ChatSystemEvent.TU_STUDENT_CONFIRMED),
                contains("Студент подтвердил ТУ"));
    }

    private static RequestEnt buildRequest(ResultEnum result) {
        RequestEnt r = new RequestEnt();
        r.setId(1L);
        r.setResult(result);
        StudentEnt student = new StudentEnt();
        student.setId(UUID.randomUUID());
        r.setStudent(student);
        ru.ai.sin.logic.recruiter.RecruiterEnt recruiter = new ru.ai.sin.logic.recruiter.RecruiterEnt();
        recruiter.setId(UUID.randomUUID());
        r.setRecruiter(recruiter);
        ChatEnt chat = new ChatEnt();
        chat.setId(UUID.randomUUID());
        r.setAppChat(chat);
        return r;
    }

    private static UserEnt studentUser(UUID studentId) {
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "S", "student", "hash");
        StudentEnt student = new StudentEnt();
        student.setId(studentId);
        user.setStudent(student);
        return user;
    }
}
