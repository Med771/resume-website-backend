package ru.ai.sin.logic.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.security.access.AccessDeniedException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.ai.sin.models.PageResponse;

import ru.ai.sin.logic.chat.ChatEnt;
import ru.ai.sin.logic.chat.ChatService;
import ru.ai.sin.logic.chat.ChatSystemEvent;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.recruiter.dto.AddRecruiterReq;
import ru.ai.sin.logic.request.dto.*;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;

import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.helper.SecurityHelper;

import ru.ai.sin.models.enums.ResultEnum;
import ru.ai.sin.models.enums.RoleEnum;
import ru.ai.sin.models.enums.UserInboxNotificationType;

import ru.ai.sin.tools.RecruiterTools;
import ru.ai.sin.tools.RequestTools;
import ru.ai.sin.tools.StudentTools;
import ru.ai.sin.tools.UserTools;
import ru.ai.sin.helper.ParticipantDisplayNames;
import ru.ai.sin.logic.notification.UserInboxNotificationService;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepo requestRepo;

    private final RequestTools requestTools;
    private final StudentTools studentTools;
    private final RecruiterTools recruiterTools;

    private final UserRepo userRepo;

    private final UserTools userTools;

    private final SecurityHelper securityHelper;

    private final ChatService chatService;

    private final AccountAccessHelper accountAccessHelper;
    private final UserInboxNotificationService inboxNotificationService;

    @Override
    @Transactional(readOnly = true)
    public RequestDTO getById(long id) {
        RequestEnt requestEnt = requestTools.getRequestOrThrow(id);

        return requestTools.mapToDTO(requestEnt);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RequestDTO> getByFilter(Pageable pageable, FilterRequestReq filterRequestReq) {
        Page<RequestEnt> page = requestRepo
                .findAll(
                        RequestSpecifications.byFilters(filterRequestReq),
                        pageable
                );

        return new PageResponse<>(
                page.getContent().stream().map(requestTools::mapToDTO).toList(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RequestDTO> getMineByFilter(Pageable pageable, FilterRequestReq filterRequestReq) {
        UserEnt user = userTools.findCurrentUserFetchingLinks()
                .orElseThrow(() -> new AccessDeniedException("Требуется авторизация"));

        FilterRequestReq scoped = filterRequestReq != null ? filterRequestReq : new FilterRequestReq(null, null, null);
        if (user.getRole() == RoleEnum.STUDENT) {
            if (user.getStudent() == null) {
                throw new BadRequestException("К аккаунту не привязана карточка студента");
            }
            scoped = new FilterRequestReq(scoped.results(), null, user.getStudent().getId());
        } else if (user.getRole() == RoleEnum.RECRUITER) {
            if (user.getRecruiter() == null) {
                throw new BadRequestException("К аккаунту не привязан профиль рекрутёра");
            }
            scoped = new FilterRequestReq(scoped.results(), user.getRecruiter().getId(), null);
        } else {
            throw new AccessDeniedException("Доступно только студентам и рекрутёрам");
        }

        return getByFilter(pageable, scoped);
    }

    @Override
    @Transactional
    public RequestDTO create(AddRequestReq addRequestReq) {
        accountAccessHelper.requireApprovedAccount();
        Optional<UserEnt> currentUserOpt = userTools.findCurrentUserFetchingLinks();
        currentUserOpt.ifPresent(u -> {
            if (u.getRole() == RoleEnum.STUDENT) {
                throw new AccessDeniedException("Студенты не могут отправлять заявки");
            }
        });

        RecruiterEnt recruiterEnt = resolveRecruiterForNewRequest(addRequestReq, currentUserOpt);

        StudentEnt studentEnt = studentTools.getStudentOrThrow(addRequestReq.studentId());
        if (!studentEnt.isCatalogVisible() && !securityHelper.isCurrentUserAdmin()) {
            throw new NotFoundException("Failed to find student by id " + addRequestReq.studentId());
        }

        ChatEnt chat = chatService.getOrCreateChat(recruiterEnt, studentEnt);

        RequestEnt requestEnt = new RequestEnt();
        requestEnt.setRecruiter(recruiterEnt);
        requestEnt.setStudent(studentEnt);
        requestEnt.setAppChat(chat);
        requestEnt.setResult(ResultEnum.WAITING);

        try {
            requestEnt = requestRepo.save(requestEnt);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Error while creating request: {}", ex.getMessage());
            throw new BadRequestException("Error while creating request");
        }

        chatService.postSystemMessage(
                chat,
                ChatSystemEvent.REQUEST_SENT,
                "Заявка №" + requestEnt.getId() + " отправлена. Ожидается решение студента."
        );

        String preview = "Новая заявка №" + requestEnt.getId();
        long createdRequestId = requestEnt.getId();
        inboxNotificationService.userIdForStudent(studentEnt).ifPresent(userId ->
                inboxNotificationService.notifyUser(
                        userId,
                        UserInboxNotificationType.NEW_REQUEST,
                        chat.getId(),
                        createdRequestId,
                        null,
                        preview,
                        ChatSystemEvent.REQUEST_SENT,
                        ParticipantDisplayNames.recruiter(recruiterEnt)
                ));

        RequestDTO requestDTO = requestTools.mapToDTO(requestEnt);
        log.info("Created new request: {} for recruiter: {} and student: {}",
                requestEnt.getId(), recruiterEnt.getId(), studentEnt.getId());

        return requestDTO;
    }

    @Override
    @Transactional
    public void studentRespond(long requestId, StudentRequestDecisionReq req) {
        UserEnt user = userTools.findCurrentUserFetchingLinks()
                .orElseThrow(() -> new AccessDeniedException("Требуется авторизация"));
        if (user.getRole() != RoleEnum.STUDENT || user.getStudent() == null) {
            throw new AccessDeniedException("Только студент может ответить по заявке");
        }
        RequestEnt r = requestTools.getRequestOrThrow(requestId);
        if (!user.getStudent().getId().equals(r.getStudent().getId())) {
            throw new AccessDeniedException("Это не ваша заявка");
        }
        if (!isPendingStudentDecision(r.getResult())) {
            throw new BadRequestException("По заявке уже принято решение");
        }
        if (req.accept() == null) {
            throw new BadRequestException("Укажите accept: true или false");
        }
        if (req.accept()) {
            r.setResult(ResultEnum.STUDENT_CONFIRMED);
        } else {
            r.setResult(ResultEnum.REFUSAL);
        }
        r.setStudentResponseText(req.comment());
        requestRepo.save(r);
        ChatEnt chat = r.getAppChat();
        if (req.accept()) {
            String body = "Студент принял заявку №" + requestId + ".";
            chatService.postSystemMessage(chat, ChatSystemEvent.STUDENT_ACCEPTED, body);
            inboxNotificationService.userIdForRecruiter(r.getRecruiter()).ifPresent(userId ->
                    inboxNotificationService.notifyUser(
                            userId,
                            UserInboxNotificationType.REQUEST_DECISION,
                            chat.getId(),
                            requestId,
                            null,
                            body,
                            ChatSystemEvent.STUDENT_ACCEPTED,
                            ParticipantDisplayNames.student(r.getStudent())
                    ));
        } else {
            String body = "Студент отклонил заявку №" + requestId + ".";
            chatService.postSystemMessage(chat, ChatSystemEvent.STUDENT_REJECTED, body);
            inboxNotificationService.userIdForRecruiter(r.getRecruiter()).ifPresent(userId ->
                    inboxNotificationService.notifyUser(
                            userId,
                            UserInboxNotificationType.REQUEST_DECISION,
                            chat.getId(),
                            requestId,
                            null,
                            body,
                            ChatSystemEvent.STUDENT_REJECTED,
                            ParticipantDisplayNames.student(r.getStudent())
                    ));
        }
    }

    private boolean isPendingStudentDecision(ResultEnum result) {
        return result == ResultEnum.WAITING || result == ResultEnum.CREATION;
    }

    /**
     * Рекрутер из привязки к аккаунту или создание/поиск по телу заявки с последующей привязкой к пользователю.
     */
    private RecruiterEnt resolveRecruiterForNewRequest(
            AddRequestReq addRequestReq,
            Optional<UserEnt> currentUserOpt
    ) {
        return currentUserOpt
                .map(UserEnt::getRecruiter)
                .map(Optional::ofNullable)
                .flatMap(o -> o)
                .orElseGet(() -> createRecruiterAndLinkForRequest(addRequestReq, currentUserOpt));
    }

    private RecruiterEnt createRecruiterAndLinkForRequest(
            AddRequestReq addRequestReq,
            Optional<UserEnt> currentUserOpt
    ) {
        validateRecruiterPayloadWhenUnlinked(addRequestReq);
        AddRecruiterReq addRecruiterReq = new AddRecruiterReq(
                addRequestReq.companyName(),
                addRequestReq.firstName(),
                addRequestReq.lastName(),
                addRequestReq.email(),
                addRequestReq.phoneNumber(),
                addRequestReq.telegramUsername()
        );
        RecruiterEnt recruiterEnt = recruiterTools.findOrCreateRecruiter(addRecruiterReq);
        currentUserOpt.ifPresent(u -> linkRecruiterToUserIfNeeded(u, recruiterEnt));
        return recruiterEnt;
    }

    @Override
    @Transactional
    public void deleteById(long id) {
        RequestEnt requestEnt = requestTools.getRequestOrThrow(id);

        try {
            requestRepo.delete(requestEnt);
        }
        catch (DataIntegrityViolationException ex) {
            log.warn("Error while deleting request: {}", ex.getMessage());

            throw new BadRequestException("Error while deleting request");
        }

        log.info("User: {}, deleted a request: {} with data: {}", securityHelper.getCurrentUsername(), id, requestEnt);
    }

    private void validateRecruiterPayloadWhenUnlinked(AddRequestReq addRequestReq) {
        if (addRequestReq.companyName() == null || addRequestReq.companyName().isBlank()) {
            throw new BadRequestException(
                    "Укажите companyName, пока к аккаунту не привязан профиль рекрутера (GET /recruiter/me)");
        }
    }

    private void linkRecruiterToUserIfNeeded(UserEnt user, RecruiterEnt recruiterEnt) {
        if (user.getRecruiter() != null) {
            return;
        }
        var existingOwner = userRepo.findByRecruiter_Id(recruiterEnt.getId());
        if (existingOwner.isPresent() && !existingOwner.get().getId().equals(user.getId())) {
            throw new BadRequestException(
                    "Этот профиль рекрутера уже привязан к другому аккаунту");
        }
        user.setRecruiter(recruiterEnt);
        userRepo.save(user);
        log.info("Linked recruiter {} to user {}", recruiterEnt.getId(), user.getId());
    }
}
