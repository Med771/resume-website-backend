package ru.ai.sin.logic.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.FileHelper;
import ru.ai.sin.helper.ParticipantDisplayNames;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.helper.TuPhaseResolver;
import ru.ai.sin.logic.chat.dto.ChatContextDTO;
import ru.ai.sin.logic.chat.dto.ChatMessageDTO;
import ru.ai.sin.logic.chat.dto.ChatSummaryDTO;
import ru.ai.sin.logic.chat.dto.MarkChatReadReq;
import ru.ai.sin.logic.chat.dto.PatchChatMessageReq;
import ru.ai.sin.logic.chat.dto.PostChatMessageReq;
import ru.ai.sin.logic.chat.event.ChatMessagePublishedEvent;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.chat.MessagingGateService;
import ru.ai.sin.logic.notification.UserInboxNotificationService;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.logic.request.RequestEnt;
import ru.ai.sin.logic.request.RequestRepo;
import ru.ai.sin.logic.request.dto.RequestDTO;
import ru.ai.sin.logic.vacancy.VacancyApplicationEnt;
import ru.ai.sin.logic.vacancy.VacancyApplicationRepo;
import ru.ai.sin.logic.profile.ProfileCommunicationGateService;
import ru.ai.sin.logic.vacancy.dto.VacancyApplicationDTO;
import ru.ai.sin.models.enums.UserInboxNotificationType;
import ru.ai.sin.tools.RequestTools;
import ru.ai.sin.tools.VacancyApplicationTools;
import ru.ai.sin.models.enums.ChatMessageKind;
import ru.ai.sin.models.enums.ResultEnum;
import ru.ai.sin.models.enums.TuPhase;
import ru.ai.sin.models.enums.RoleEnum;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final LocalDateTime EPOCH_READ = LocalDateTime.of(1970, 1, 1, 0, 0);

    private final ChatRepo chatRepo;
    private final ChatMessageRepo chatMessageRepo;
    private final ChatReadStateRepo chatReadStateRepo;
    private final MessagingGateService messagingGateService;
    private final ProfileCommunicationGateService profileCommunicationGateService;
    private final UserRepo userRepo;
    private final SecurityHelper securityHelper;
    private final ApplicationEventPublisher eventPublisher;
    private final FileHelper fileHelper;
    private final RequestRepo requestRepo;
    private final RequestTools requestTools;
    private final VacancyApplicationRepo vacancyApplicationRepo;
    private final VacancyApplicationTools vacancyApplicationTools;
    private final UserInboxNotificationService inboxNotificationService;

    @Override
    @Transactional
    public ChatEnt getOrCreateChat(RecruiterEnt recruiter, StudentEnt student) {
        return chatRepo.findByRecruiter_IdAndStudent_Id(recruiter.getId(), student.getId())
                .orElseGet(() -> {
                    ChatEnt c = new ChatEnt();
                    c.setRecruiter(recruiter);
                    c.setStudent(student);
                    c.setLastActivityAt(LocalDateTime.now());
                    return chatRepo.save(c);
                });
    }

    @Override
    @Transactional
    public void postSystemMessage(ChatEnt chat, String systemEvent, String body) {
        ChatEnt managed = chatRepo.findById(chat.getId())
                .orElseThrow(() -> new NotFoundException("Chat not found"));
        ChatMessageEnt m = new ChatMessageEnt();
        m.setChat(managed);
        m.setMessageKind(ChatMessageKind.SYSTEM);
        m.setSystemEvent(systemEvent);
        m.setBody(body);
        m = chatMessageRepo.save(m);
        touch(managed);
        eventPublisher.publishEvent(new ChatMessagePublishedEvent(managed.getId(), toDto(m)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatSummaryDTO> listMyChats(Pageable pageable) {
        UserEnt user = requireUserWithLinks();
        requireCommunicationReady(user);
        Page<ChatEnt> page;
        if (user.getRole() == RoleEnum.ADMIN) {
            page = chatRepo.findAllByOrderByLastActivityAtDesc(pageable);
        } else if (user.getRecruiter() != null) {
            page = chatRepo.findByRecruiter_IdOrderByLastActivityAtDesc(user.getRecruiter().getId(), pageable);
        } else if (user.getStudent() != null) {
            page = chatRepo.findByStudent_IdOrderByLastActivityAtDesc(user.getStudent().getId(), pageable);
        } else {
            return Page.empty(pageable);
        }
        return page.map(c -> toSummary(c, user));
    }

    @Override
    @Transactional(readOnly = true)
    public ChatSummaryDTO getChatSummary(UUID chatId) {
        UserEnt user = requireUserWithLinks();
        requireCommunicationReady(user);
        ChatEnt chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Chat not found"));
        assertCanAccess(user, chat);
        return toSummary(chat, user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageDTO> listMessages(UUID chatId, Pageable pageable) {
        UserEnt user = requireUserWithLinks();
        requireCommunicationReady(user);
        ChatEnt chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Chat not found"));
        assertCanAccess(user, chat);
        boolean fullHistory = user.getRole() == RoleEnum.ADMIN || isMessagingAllowed(chat);
        Pageable paging = pagingOnly(pageable);
        return chatMessageRepo
                .findVisibleByChatIdGated(chatId, fullHistory, ChatMessageKind.SYSTEM, paging)
                .map(this::toDto);
    }

    @Override
    @Transactional
    public ChatMessageDTO sendTextMessage(UUID chatId, PostChatMessageReq req) {
        UserEnt user = requireUserWithLinks();
        requireCommunicationReady(user);
        ChatEnt chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Chat not found"));
        assertCanAccess(user, chat);
        assertCanPostUserMessage(user, chat);

        if (user.getRole() == RoleEnum.ADMIN
                && chatMessageRepo.existsUserMessageFromRole(
                chatId, ChatMessageKind.USER, RoleEnum.ADMIN)) {
            postSystemMessage(chat, ChatSystemEvent.ADMIN_JOINED, "Администратор подключился к диалогу.");
        }

        String text = req.body() == null ? "" : req.body().trim();
        if (!StringUtils.hasText(text)) {
            throw new BadRequestException("Текст сообщения не может быть пустым");
        }

        ChatEnt fresh = chatRepo.findById(chatId).orElseThrow();
        return persistUserMessage(fresh, user, text);
    }

    @Override
    @Transactional
    public ChatMessageDTO sendMessageWithAttachment(UUID chatId, String body, MultipartFile file) {
        UserEnt user = requireUserWithLinks();
        requireCommunicationReady(user);
        ChatEnt chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Chat not found"));
        assertCanAccess(user, chat);
        assertCanPostUserMessage(user, chat);

        if (user.getRole() == RoleEnum.ADMIN
                && chatMessageRepo.existsUserMessageFromRole(
                chatId, ChatMessageKind.USER, RoleEnum.ADMIN)) {
            postSystemMessage(chat, ChatSystemEvent.ADMIN_JOINED, "Администратор подключился к диалогу.");
        }

        fileHelper.validateMultipart(file);
        String text = body == null ? "" : body.trim();
        if (!StringUtils.hasText(text)) {
            text = " ";
        }

        ChatEnt fresh = chatRepo.findById(chatId).orElseThrow();

        ChatMessageEnt m = new ChatMessageEnt();
        m.setChat(fresh);
        m.setAuthor(user);
        m.setMessageKind(ChatMessageKind.USER);
        m.setBody(text.trim());
        m = chatMessageRepo.save(m);

        String stored = fileHelper.saveFile(file, "chatatt-" + m.getId());
        if (stored == null) {
            throw new BadRequestException("Не удалось сохранить вложение");
        }
        m.setAttachmentStorageName(stored);
        m = chatMessageRepo.save(m);
        touch(fresh);
        ChatMessageDTO dto = toDto(m);
        eventPublisher.publishEvent(new ChatMessagePublishedEvent(fresh.getId(), dto));
        notifyCounterpartyAboutMessage(fresh, user, dto);
        return dto;
    }

    @Override
    @Transactional
    public ChatMessageDTO editMessage(UUID chatId, UUID messageId, PatchChatMessageReq req) {
        UserEnt user = requireUserWithLinks();
        requireCommunicationReady(user);
        ChatEnt chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Chat not found"));
        assertCanAccess(user, chat);
        ChatMessageEnt m = chatMessageRepo.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Сообщение не найдено"));
        if (!m.getChat().getId().equals(chatId)) {
            throw new BadRequestException("Сообщение не из этого чата");
        }
        if (m.getDeletedAt() != null) {
            throw new BadRequestException("Нельзя редактировать удалённое сообщение");
        }
        if (m.getMessageKind() != ChatMessageKind.USER) {
            throw new BadRequestException("Редактировать можно только пользовательские сообщения");
        }
        boolean author = m.getAuthor() != null && m.getAuthor().getId().equals(user.getId());
        if (!author && user.getRole() != RoleEnum.ADMIN) {
            throw new AccessDeniedException("Можно редактировать только свои сообщения");
        }
        m.setBody(req.body());
        m.setEditedAt(LocalDateTime.now());
        m = chatMessageRepo.save(m);
        touch(chat);
        ChatMessageDTO dto = toDto(m);
        eventPublisher.publishEvent(new ChatMessagePublishedEvent(chatId, dto));
        return dto;
    }

    @Override
    @Transactional
    public void adminSoftDeleteMessage(UUID chatId, UUID messageId) {
        UserEnt user = requireUserWithLinks();
        if (user.getRole() != RoleEnum.ADMIN) {
            throw new AccessDeniedException("Удаление сообщений доступно только администратору");
        }
        ChatEnt chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Chat not found"));
        ChatMessageEnt m = chatMessageRepo.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Сообщение не найдено"));
        if (!m.getChat().getId().equals(chatId)) {
            throw new BadRequestException("Сообщение не из этого чата");
        }
        m.setDeletedAt(LocalDateTime.now());
        m.setDeletedByAdmin(true);
        m.setBody(null);
        m.setAttachmentStorageName(null);
        chatMessageRepo.save(m);
        touch(chat);
        eventPublisher.publishEvent(new ChatMessagePublishedEvent(chatId, toDto(m)));
    }

    @Override
    @Transactional
    public void markRead(UUID chatId, MarkChatReadReq req) {
        UserEnt user = requireUserWithLinks();
        requireCommunicationReady(user);
        ChatEnt chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Chat not found"));
        assertCanAccess(user, chat);
        ChatMessageEnt m = chatMessageRepo.findById(req.messageId())
                .orElseThrow(() -> new NotFoundException("Сообщение не найдено"));
        if (!m.getChat().getId().equals(chatId)) {
            throw new BadRequestException("Сообщение не из этого чата");
        }
        LocalDateTime readAt = m.getTimestamps().getCreatedAt() != null
                ? m.getTimestamps().getCreatedAt()
                : LocalDateTime.now();

        ChatReadStateEnt state = chatReadStateRepo.findByChatIdAndUserId(chatId, user.getId())
                .orElseGet(() -> {
                    ChatReadStateEnt s = new ChatReadStateEnt();
                    s.setChatId(chatId);
                    s.setUserId(user.getId());
                    return s;
                });
        state.setLastReadMessageId(m.getId());
        state.setLastReadAt(readAt);
        chatReadStateRepo.save(state);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatContextDTO getChatContext(UUID chatId) {
        UserEnt user = requireUserWithLinks();
        if (user.getRole() != RoleEnum.ADMIN) {
            throw new AccessDeniedException("Контекст чата доступен только администратору");
        }
        ChatEnt chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Chat not found"));
        ChatSummaryDTO summary = toSummary(chat, user);
        List<RequestDTO> requests = requestRepo.findByAppChat_IdOrderByIdDesc(chatId).stream()
                .map(requestTools::mapToDTO)
                .toList();
        List<VacancyApplicationDTO> applications = vacancyApplicationRepo.findByAppChat_IdWithDetails(chatId).stream()
                .map(vacancyApplicationTools::mapToDTO)
                .toList();
        return new ChatContextDTO(chatId, summary, requests, applications);
    }

    @Override
    @Transactional
    public void adminDeleteChat(UUID chatId) {
        UserEnt user = requireUserWithLinks();
        if (user.getRole() != RoleEnum.ADMIN) {
            throw new AccessDeniedException("Удаление чата доступно только администратору");
        }
        if (!chatRepo.existsById(chatId)) {
            throw new NotFoundException("Chat not found");
        }
        requestRepo.deleteByAppChat_Id(chatId);
        vacancyApplicationRepo.deleteByAppChat_Id(chatId);
        chatRepo.deleteById(chatId);
        log.info("Admin {} deleted chat {} with cascade", user.getUsername(), chatId);
    }

    private ChatMessageDTO persistUserMessage(ChatEnt chat, UserEnt user, String text) {
        ChatMessageEnt m = new ChatMessageEnt();
        m.setChat(chat);
        m.setAuthor(user);
        m.setMessageKind(ChatMessageKind.USER);
        m.setBody(text);
        m.setAttachmentStorageName(null);
        m = chatMessageRepo.save(m);
        touch(chat);
        ChatMessageDTO dto = toDto(m);
        eventPublisher.publishEvent(new ChatMessagePublishedEvent(chat.getId(), dto));
        notifyCounterpartyAboutMessage(chat, user, dto);
        return dto;
    }

    private void notifyCounterpartyAboutMessage(ChatEnt chat, UserEnt author, ChatMessageDTO dto) {
        if (author.getRole() == RoleEnum.ADMIN) {
            inboxNotificationService.userIdForStudent(chat.getStudent()).ifPresent(id ->
                    notifyChatMessage(id, chat, author, dto));
            inboxNotificationService.userIdForRecruiter(chat.getRecruiter()).ifPresent(id ->
                    notifyChatMessage(id, chat, author, dto));
            return;
        }
        UUID recipientId = null;
        String counterpartyName = null;
        if (author.getStudent() != null
                && author.getStudent().getId().equals(chat.getStudent().getId())) {
            recipientId = inboxNotificationService.userIdForRecruiter(chat.getRecruiter()).orElse(null);
            counterpartyName = ParticipantDisplayNames.student(chat.getStudent());
        } else if (author.getRecruiter() != null
                && author.getRecruiter().getId().equals(chat.getRecruiter().getId())) {
            recipientId = inboxNotificationService.userIdForStudent(chat.getStudent()).orElse(null);
            counterpartyName = ParticipantDisplayNames.recruiter(chat.getRecruiter());
        }
        if (recipientId != null) {
            inboxNotificationService.notifyUser(
                    recipientId,
                    UserInboxNotificationType.CHAT_MESSAGE,
                    chat.getId(),
                    null,
                    null,
                    messagePreview(dto.body(), dto.attachmentStorageName()),
                    null,
                    counterpartyName
            );
        }
    }

    private void notifyChatMessage(UUID recipientId, ChatEnt chat, UserEnt author, ChatMessageDTO dto) {
        if (recipientId.equals(author.getId())) {
            return;
        }
        inboxNotificationService.notifyUser(
                recipientId,
                UserInboxNotificationType.CHAT_MESSAGE,
                chat.getId(),
                null,
                null,
                messagePreview(dto.body(), dto.attachmentStorageName()),
                null,
                author.getUsername()
        );
    }

    private String messagePreview(String body, String attachment) {
        String b = body != null ? body : "";
        if (StringUtils.hasText(attachment)) {
            b = b + " 📎";
        }
        return truncate(b);
    }

    private void requireCommunicationReady(UserEnt user) {
        if (user.getRole() == RoleEnum.ADMIN) {
            return;
        }
        profileCommunicationGateService.requireReadyForCommunication();
    }

    private void assertCanPostUserMessage(UserEnt user, ChatEnt chat) {
        if (user.getRole() == RoleEnum.ADMIN) {
            return;
        }
        if (!isMessagingAllowed(chat)) {
            throw new BadRequestException("Переписка доступна после принятия заявки студентом.");
        }
    }

    private boolean isMessagingAllowed(ChatEnt chat) {
        return messagingGateService.isMessagingAllowed(
                chat.getRecruiter().getId(),
                chat.getStudent().getId()
        );
    }

    private void touch(ChatEnt chat) {
        chat.setLastActivityAt(LocalDateTime.now());
        chatRepo.save(chat);
    }

    private UserEnt requireUserWithLinks() {
        String username = securityHelper.getCurrentUsername();
        return userRepo.findByUsernameFetchingLinks(username)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    private void assertCanAccess(UserEnt user, ChatEnt chat) {
        if (user.getRole() == RoleEnum.ADMIN) {
            return;
        }
        if (user.getRecruiter() != null
                && user.getRecruiter().getId().equals(chat.getRecruiter().getId())) {
            return;
        }
        if (user.getStudent() != null
                && user.getStudent().getId().equals(chat.getStudent().getId())) {
            return;
        }
        throw new AccessDeniedException("Нет доступа к этому чату");
    }

    private ChatSummaryDTO toSummary(ChatEnt chat, UserEnt viewer) {
        boolean fullHistory = viewer.getRole() == RoleEnum.ADMIN || isMessagingAllowed(chat);
        Page<ChatMessageEnt> last = chatMessageRepo.findLastByChatIdGated(
                chat.getId(), fullHistory, ChatMessageKind.SYSTEM, PageRequest.of(0, 1));
        String preview = "";
        if (last.hasContent()) {
            ChatMessageEnt lm = last.getContent().getFirst();
            preview = previewOf(lm);
        }
        long unread = countUnread(chat.getId(), viewer.getId());
        RequestEnt activeRequest = findActiveRequest(chat.getId());
        Long activeRequestId = activeRequest != null ? activeRequest.getId() : null;
        ResultEnum activeRequestResult = activeRequest != null ? activeRequest.getResult() : null;
        TuPhase tuPhase = activeRequest != null
                ? TuPhaseResolver.fromRequest(activeRequest)
                : TuPhase.NOT_APPLICABLE;
        long messageCount = chatMessageRepo.countByChat_IdAndDeletedAtIsNull(chat.getId());
        return new ChatSummaryDTO(
                chat.getId(),
                chat.getRecruiter().getId(),
                chat.getStudent().getId(),
                preview,
                chat.getLastActivityAt(),
                unread,
                ParticipantDisplayNames.recruiter(chat.getRecruiter()),
                ParticipantDisplayNames.student(chat.getStudent()),
                activeRequestId,
                activeRequestResult,
                tuPhase,
                messageCount
        );
    }

    private RequestEnt findActiveRequest(UUID chatId) {
        List<RequestEnt> requests = requestRepo.findByAppChat_IdOrderByIdDesc(chatId);
        return requests.isEmpty() ? null : requests.getFirst();
    }

    private String previewOf(ChatMessageEnt m) {
        if (m.getDeletedAt() != null) {
            return "[удалено]";
        }
        if (m.getMessageKind() == ChatMessageKind.SYSTEM) {
            return m.getBody() != null ? m.getBody() : "";
        }
        String b = m.getBody() != null ? m.getBody() : "";
        if (StringUtils.hasText(m.getAttachmentStorageName())) {
            b = b + " 📎";
        }
        return truncate(b);
    }

    private long countUnread(UUID chatId, UUID userId) {
        LocalDateTime after = chatReadStateRepo.findByChatIdAndUserId(chatId, userId).filter(rs -> rs.getLastReadAt() != null).map(ChatReadStateEnt::getLastReadAt).orElse(EPOCH_READ);
        return chatMessageRepo.countIncomingUnreadAfter(chatId, after, userId);
    }

    private String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 160 ? s : s.substring(0, 160 - 1) + "…";
    }

    /**
     * Клиенты передают {@code sort=createdAt}, но в JPA поле — {@code timestamps.createdAt}.
     * Сортировка уже задана в {@link ChatMessageRepo}; лишний Sort из Pageable ломает запрос.
     */
    private static Pageable pagingOnly(Pageable pageable) {
        if (pageable.isUnpaged()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }

    private ChatMessageDTO toDto(ChatMessageEnt m) {
        UUID authorId = m.getAuthor() != null ? m.getAuthor().getId() : null;
        String authorName = m.getAuthor() != null ? m.getAuthor().getUsername() : null;
        return new ChatMessageDTO(
                m.getId(),
                m.getChat().getId(),
                authorId,
                authorName,
                m.getMessageKind(),
                m.getSystemEvent(),
                m.getBody(),
                m.getAttachmentStorageName(),
                m.getTimestamps().getCreatedAt(),
                m.getTimestamps().getUpdatedAt(),
                m.getEditedAt(),
                m.getDeletedAt(),
                m.isDeletedByAdmin()
        );
    }
}
