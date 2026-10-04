package ru.ai.sin.logic.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.helper.FileHelper;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.chat.dto.PatchChatMessageReq;
import ru.ai.sin.logic.chat.dto.PostChatMessageReq;
import ru.ai.sin.logic.profile.ProfileCommunicationGateService;
import ru.ai.sin.models.embeddables.TimeStamped;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.logic.notification.UserInboxNotificationService;
import ru.ai.sin.logic.request.RequestRepo;
import ru.ai.sin.logic.vacancy.VacancyApplicationRepo;
import ru.ai.sin.tools.RequestTools;
import ru.ai.sin.tools.VacancyApplicationTools;
import ru.ai.sin.models.enums.ChatMessageKind;
import ru.ai.sin.models.enums.RoleEnum;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatRepo chatRepo;
    @Mock
    private ChatMessageRepo chatMessageRepo;
    @Mock
    private ChatReadStateRepo chatReadStateRepo;
    @Mock
    private MessagingGateService messagingGateService;
    @Mock
    private ProfileCommunicationGateService profileCommunicationGateService;
    @Mock
    private UserRepo userRepo;
    @Mock
    private SecurityHelper securityHelper;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private FileHelper fileHelper;
    @Mock
    private RequestRepo requestRepo;
    @Mock
    private RequestTools requestTools;
    @Mock
    private VacancyApplicationRepo vacancyApplicationRepo;
    @Mock
    private VacancyApplicationTools vacancyApplicationTools;
    @Mock
    private UserInboxNotificationService inboxNotificationService;

    private ChatServiceImpl chatService;

    private final UUID chatId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID recruiterId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private final UUID studentId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private final UUID userId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @BeforeEach
    void setUp() {
        chatService = new ChatServiceImpl(
                chatRepo,
                chatMessageRepo,
                chatReadStateRepo,
                messagingGateService,
                profileCommunicationGateService,
                userRepo,
                securityHelper,
                eventPublisher,
                fileHelper,
                requestRepo,
                requestTools,
                vacancyApplicationRepo,
                vacancyApplicationTools,
                inboxNotificationService
        );
    }

    @Test
    void listMessages_usesGatedQueryWithPartialHistoryForRecruiterUntilRequestAccepted() {
        when(securityHelper.getCurrentUsername()).thenReturn("rec1");

        RecruiterEnt recruiter = new RecruiterEnt();
        recruiter.setId(recruiterId);
        UserEnt user = new UserEnt(RoleEnum.RECRUITER, "R", "rec1", "h");
        user.setId(userId);
        user.setRecruiter(recruiter);
        when(userRepo.findByUsernameFetchingLinks("rec1")).thenReturn(Optional.of(user));

        RecruiterEnt chatRecruiter = new RecruiterEnt();
        chatRecruiter.setId(recruiterId);
        StudentEnt chatStudent = new StudentEnt();
        chatStudent.setId(studentId);
        ChatEnt chat = new ChatEnt();
        chat.setId(chatId);
        chat.setRecruiter(chatRecruiter);
        chat.setStudent(chatStudent);
        when(chatRepo.findById(chatId)).thenReturn(Optional.of(chat));

        when(messagingGateService.isMessagingAllowed(recruiterId, studentId)).thenReturn(false);

        Pageable pageable = PageRequest.of(0, 20);
        when(chatMessageRepo.findVisibleByChatIdGated(chatId, false, ChatMessageKind.SYSTEM, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of()));

        chatService.listMessages(chatId, pageable);

        verify(chatMessageRepo).findVisibleByChatIdGated(chatId, false, ChatMessageKind.SYSTEM, PageRequest.of(0, 20));
    }

    @Test
    void listMessages_ignoresClientSortCreatedAt() {
        when(securityHelper.getCurrentUsername()).thenReturn("stu1");

        StudentEnt student = new StudentEnt();
        student.setId(studentId);
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "S", "stu1", "h");
        user.setId(userId);
        user.setStudent(student);
        when(userRepo.findByUsernameFetchingLinks("stu1")).thenReturn(Optional.of(user));

        RecruiterEnt chatRecruiter = new RecruiterEnt();
        chatRecruiter.setId(recruiterId);
        StudentEnt chatStudent = new StudentEnt();
        chatStudent.setId(studentId);
        ChatEnt chat = new ChatEnt();
        chat.setId(chatId);
        chat.setRecruiter(chatRecruiter);
        chat.setStudent(chatStudent);
        when(chatRepo.findById(chatId)).thenReturn(Optional.of(chat));
        when(messagingGateService.isMessagingAllowed(recruiterId, studentId)).thenReturn(true);

        Pageable withSort = PageRequest.of(0, 20, Sort.by("createdAt"));
        when(chatMessageRepo.findVisibleByChatIdGated(chatId, true, ChatMessageKind.SYSTEM, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of()));

        chatService.listMessages(chatId, withSort);

        verify(chatMessageRepo).findVisibleByChatIdGated(chatId, true, ChatMessageKind.SYSTEM, PageRequest.of(0, 20));
    }

    @Test
    void sendTextMessage_blocksRecruiterWhenMessagingNotYetAllowed() {
        when(securityHelper.getCurrentUsername()).thenReturn("rec1");

        RecruiterEnt recruiter = new RecruiterEnt();
        recruiter.setId(recruiterId);
        UserEnt user = new UserEnt(RoleEnum.RECRUITER, "R", "rec1", "h");
        user.setRecruiter(recruiter);
        when(userRepo.findByUsernameFetchingLinks("rec1")).thenReturn(Optional.of(user));

        RecruiterEnt chatRecruiter = new RecruiterEnt();
        chatRecruiter.setId(recruiterId);
        StudentEnt chatStudent = new StudentEnt();
        chatStudent.setId(studentId);
        ChatEnt chat = new ChatEnt();
        chat.setId(chatId);
        chat.setRecruiter(chatRecruiter);
        chat.setStudent(chatStudent);
        when(chatRepo.findById(chatId)).thenReturn(Optional.of(chat));

        when(messagingGateService.isMessagingAllowed(recruiterId, studentId)).thenReturn(false);

        assertThatThrownBy(() -> chatService.sendTextMessage(chatId, new PostChatMessageReq("Привет")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Переписка доступна после принятия заявки");
    }

    @Test
    void getChatSummary_throwsWhenUserHasNoAccess() {
        when(securityHelper.getCurrentUsername()).thenReturn("stranger");

        RecruiterEnt otherRec = new RecruiterEnt();
        otherRec.setId(UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"));
        UserEnt user = new UserEnt(RoleEnum.RECRUITER, "X", "stranger", "h");
        user.setRecruiter(otherRec);
        when(userRepo.findByUsernameFetchingLinks("stranger")).thenReturn(Optional.of(user));

        RecruiterEnt chatRecruiter = new RecruiterEnt();
        chatRecruiter.setId(recruiterId);
        StudentEnt chatStudent = new StudentEnt();
        chatStudent.setId(studentId);
        ChatEnt chat = new ChatEnt();
        chat.setId(chatId);
        chat.setRecruiter(chatRecruiter);
        chat.setStudent(chatStudent);
        when(chatRepo.findById(chatId)).thenReturn(Optional.of(chat));

        assertThatThrownBy(() -> chatService.getChatSummary(chatId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Нет доступа");
    }

    @Test
    void listMessages_adminUsesFullHistoryInGatedQuery() {
        when(securityHelper.getCurrentUsername()).thenReturn("admin");

        UserEnt admin = new UserEnt(RoleEnum.ADMIN, "A", "admin", "h");
        when(userRepo.findByUsernameFetchingLinks("admin")).thenReturn(Optional.of(admin));

        RecruiterEnt chatRecruiter = new RecruiterEnt();
        chatRecruiter.setId(recruiterId);
        StudentEnt chatStudent = new StudentEnt();
        chatStudent.setId(studentId);
        ChatEnt chat = new ChatEnt();
        chat.setId(chatId);
        chat.setRecruiter(chatRecruiter);
        chat.setStudent(chatStudent);
        when(chatRepo.findById(chatId)).thenReturn(Optional.of(chat));

        Pageable pageable = PageRequest.of(0, 20);
        when(chatMessageRepo.findVisibleByChatIdGated(chatId, true, ChatMessageKind.SYSTEM, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of()));

        chatService.listMessages(chatId, pageable);

        verify(chatMessageRepo).findVisibleByChatIdGated(chatId, true, ChatMessageKind.SYSTEM, PageRequest.of(0, 20));
    }

    @Test
    void sendTextMessage_rejectsBlankBodyWhenMessagingAllowed() {
        when(securityHelper.getCurrentUsername()).thenReturn("rec1");

        RecruiterEnt recruiter = new RecruiterEnt();
        recruiter.setId(recruiterId);
        UserEnt user = new UserEnt(RoleEnum.RECRUITER, "R", "rec1", "h");
        user.setRecruiter(recruiter);
        when(userRepo.findByUsernameFetchingLinks("rec1")).thenReturn(Optional.of(user));

        RecruiterEnt chatRecruiter = new RecruiterEnt();
        chatRecruiter.setId(recruiterId);
        StudentEnt chatStudent = new StudentEnt();
        chatStudent.setId(studentId);
        ChatEnt chat = new ChatEnt();
        chat.setId(chatId);
        chat.setRecruiter(chatRecruiter);
        chat.setStudent(chatStudent);
        when(chatRepo.findById(chatId)).thenReturn(Optional.of(chat));

        when(messagingGateService.isMessagingAllowed(recruiterId, studentId)).thenReturn(true);

        assertThatThrownBy(() -> chatService.sendTextMessage(chatId, new PostChatMessageReq("   ")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("пустым");
    }

    @Test
    void adminSoftDeleteMessage_forbiddenForRecruiter() {
        when(securityHelper.getCurrentUsername()).thenReturn("rec1");

        RecruiterEnt recruiter = new RecruiterEnt();
        recruiter.setId(recruiterId);
        UserEnt user = new UserEnt(RoleEnum.RECRUITER, "R", "rec1", "h");
        user.setRecruiter(recruiter);
        when(userRepo.findByUsernameFetchingLinks("rec1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> chatService.adminSoftDeleteMessage(chatId, UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("только администратору");
    }

    @Test
    void editMessage_deniesNonAuthorNonAdmin() {
        UUID messageId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        UUID otherAuthorId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

        when(securityHelper.getCurrentUsername()).thenReturn("rec1");

        RecruiterEnt recruiter = new RecruiterEnt();
        recruiter.setId(recruiterId);
        UserEnt viewer = new UserEnt(RoleEnum.RECRUITER, "R", "rec1", "h");
        viewer.setId(userId);
        viewer.setRecruiter(recruiter);
        when(userRepo.findByUsernameFetchingLinks("rec1")).thenReturn(Optional.of(viewer));

        RecruiterEnt chatRecruiter = new RecruiterEnt();
        chatRecruiter.setId(recruiterId);
        StudentEnt chatStudent = new StudentEnt();
        chatStudent.setId(studentId);
        ChatEnt chat = new ChatEnt();
        chat.setId(chatId);
        chat.setRecruiter(chatRecruiter);
        chat.setStudent(chatStudent);
        when(chatRepo.findById(chatId)).thenReturn(Optional.of(chat));

        UserEnt otherAuthor = new UserEnt(RoleEnum.RECRUITER, "O", "other", "h");
        otherAuthor.setId(otherAuthorId);

        ChatMessageEnt message = new ChatMessageEnt();
        message.setId(messageId);
        message.setChat(chat);
        message.setAuthor(otherAuthor);
        message.setMessageKind(ChatMessageKind.USER);
        TimeStamped ts = new TimeStamped();
        ts.setCreatedAt(LocalDateTime.now());
        message.setTimestamps(ts);
        when(chatMessageRepo.findById(messageId)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> chatService.editMessage(chatId, messageId, new PatchChatMessageReq("new text")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("только свои");
    }
}
