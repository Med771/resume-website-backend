package ru.ai.sin.logic.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.logic.chat.ChatEnt;
import ru.ai.sin.logic.chat.ChatService;
import ru.ai.sin.logic.chat.ChatSystemEvent;
import ru.ai.sin.logic.request.dto.TuDecisionReq;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.models.enums.RejectionReasonCode;
import ru.ai.sin.models.enums.ResultEnum;
import ru.ai.sin.models.enums.RoleEnum;
import ru.ai.sin.models.enums.UserInboxNotificationType;
import ru.ai.sin.tools.RequestTools;
import ru.ai.sin.tools.UserTools;
import ru.ai.sin.helper.ParticipantDisplayNames;
import ru.ai.sin.logic.notification.UserInboxNotificationService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TuDecisionService {

    private final RequestRepo requestRepo;
    private final RequestTools requestTools;
    private final UserTools userTools;
    private final ChatService chatService;
    private final AccountAccessHelper accountAccessHelper;
    private final UserInboxNotificationService inboxNotificationService;

    @Transactional
    public void decideOnRequest(long requestId, TuDecisionReq req) {
        accountAccessHelper.requireApprovedAccount();
        RequestEnt r = requestTools.getRequestOrThrow(requestId);
        UserEnt user = userTools.findCurrentUserFetchingLinks()
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (Boolean.FALSE.equals(req.accept())) {
            applyRejection(r, req, user);
            return;
        }
        if (req.accept() == null || !req.accept()) {
            throw new BadRequestException("Укажите accept: true или false");
        }

        if (user.getRole() == RoleEnum.STUDENT) {
            confirmAsStudent(r, user);
        } else if (user.getRole() == RoleEnum.RECRUITER) {
            confirmAsRecruiter(r, user);
        } else if (user.getRole() == RoleEnum.ADMIN) {
            throw new BadRequestException("Админ не участвует в ТУ");
        } else {
            throw new BadRequestException("Недоступно для вашей роли");
        }
    }

    private void confirmAsStudent(RequestEnt r, UserEnt user) {
        if (user.getStudent() == null || !user.getStudent().getId().equals(r.getStudent().getId())) {
            throw new BadRequestException("Это не ваша заявка");
        }
        if (!isTuConfirmStage(r.getResult())) {
            throw new BadRequestException("Неверный этап заявки для ТУ");
        }
        if (r.getStudentTuConfirmedAt() != null) {
            throw new BadRequestException("Вы уже подтвердили ТУ по этой заявке");
        }
        r.setStudentTuConfirmedAt(LocalDateTime.now());
        if (r.getRecruiterTuConfirmedAt() != null) {
            r.setResult(ResultEnum.SUCCESS);
            postSuccess(r);
        } else {
            r.setResult(ResultEnum.STUDENT_CONFIRMED);
            postPartialStudentConfirmed(r);
        }
        requestRepo.save(r);
    }

    private void confirmAsRecruiter(RequestEnt r, UserEnt user) {
        if (user.getRecruiter() == null || !user.getRecruiter().getId().equals(r.getRecruiter().getId())) {
            throw new BadRequestException("Это не ваша заявка");
        }
        if (!isTuConfirmStage(r.getResult())) {
            throw new BadRequestException("Неверный этап заявки для ТУ");
        }
        if (r.getRecruiterTuConfirmedAt() != null) {
            throw new BadRequestException("Вы уже подтвердили ТУ по этой заявке");
        }
        r.setRecruiterTuConfirmedAt(LocalDateTime.now());
        if (r.getStudentTuConfirmedAt() != null) {
            r.setResult(ResultEnum.SUCCESS);
            postSuccess(r);
        } else {
            r.setResult(ResultEnum.RECRUITER_CONFIRMED);
            postPartialRecruiterConfirmed(r);
        }
        requestRepo.save(r);
    }

    private static boolean isTuConfirmStage(ResultEnum result) {
        return result == ResultEnum.STUDENT_CONFIRMED || result == ResultEnum.RECRUITER_CONFIRMED;
    }

    private void applyRejection(RequestEnt r, TuDecisionReq req, UserEnt user) {
        if (user.getRole() != RoleEnum.STUDENT && user.getRole() != RoleEnum.RECRUITER) {
            throw new BadRequestException("Недоступно для вашей роли");
        }
        if (user.getRole() == RoleEnum.STUDENT) {
            if (user.getStudent() == null || !user.getStudent().getId().equals(r.getStudent().getId())) {
                throw new BadRequestException("Это не ваша заявка");
            }
        } else {
            if (user.getRecruiter() == null || !user.getRecruiter().getId().equals(r.getRecruiter().getId())) {
                throw new BadRequestException("Это не ваша заявка");
            }
        }
        if (!isTuConfirmStage(r.getResult()) && r.getResult() != ResultEnum.SUCCESS) {
            throw new BadRequestException("Отказ по ТУ недоступен на текущем этапе заявки");
        }
        AccountAccessHelper.validateRejection(req.reasonCode(), req.comment());
        r.setRejectionReasonCode(RejectionReasonCode.fromCode(req.reasonCode()).getCode());
        r.setRejectionComment(req.comment());
        r.setResult(ResultEnum.REFUSAL);
        requestRepo.save(r);
        ChatEnt chat = r.getAppChat();
        chatService.postSystemMessage(chat, ChatSystemEvent.TU_REJECTED,
                "Отказ по заявке №" + r.getId() + ": " + req.reasonCode());
        notifyBoth(r, UserInboxNotificationType.TU_REJECTED,
                "Отказ по заявке №" + r.getId(), ChatSystemEvent.TU_REJECTED);
    }

    private void postPartialStudentConfirmed(RequestEnt r) {
        String body = "Студент подтвердил ТУ по заявке №" + r.getId() + ". Ожидается подтверждение работодателя.";
        chatService.postSystemMessage(r.getAppChat(), ChatSystemEvent.TU_STUDENT_CONFIRMED, body);
        inboxNotificationService.userIdForRecruiter(r.getRecruiter()).ifPresent(userId ->
                inboxNotificationService.notifyUser(
                        userId,
                        UserInboxNotificationType.TU_PARTIAL,
                        r.getAppChat().getId(),
                        r.getId(),
                        null,
                        body,
                        ChatSystemEvent.TU_STUDENT_CONFIRMED,
                        ParticipantDisplayNames.student(r.getStudent())
                ));
    }

    private void postPartialRecruiterConfirmed(RequestEnt r) {
        String body = "Работодатель подтвердил ТУ по заявке №" + r.getId() + ". Ожидается подтверждение студента.";
        chatService.postSystemMessage(r.getAppChat(), ChatSystemEvent.TU_RECRUITER_CONFIRMED, body);
        inboxNotificationService.userIdForStudent(r.getStudent()).ifPresent(userId ->
                inboxNotificationService.notifyUser(
                        userId,
                        UserInboxNotificationType.TU_PARTIAL,
                        r.getAppChat().getId(),
                        r.getId(),
                        null,
                        body,
                        ChatSystemEvent.TU_RECRUITER_CONFIRMED,
                        ParticipantDisplayNames.recruiter(r.getRecruiter())
                ));
    }

    private void postSuccess(RequestEnt r) {
        String body = "Обе стороны подтвердили ТУ по заявке №" + r.getId() + ". Успех.";
        chatService.postSystemMessage(r.getAppChat(), ChatSystemEvent.TU_CONFIRMED, body);
        notifyBoth(r, UserInboxNotificationType.TU_CONFIRMED, body, ChatSystemEvent.TU_CONFIRMED);
    }

    private void notifyBoth(RequestEnt r, UserInboxNotificationType type, String preview, String systemEvent) {
        inboxNotificationService.userIdForStudent(r.getStudent()).ifPresent(userId ->
                inboxNotificationService.notifyUser(
                        userId, type, r.getAppChat().getId(), r.getId(), null, preview, systemEvent,
                        ParticipantDisplayNames.recruiter(r.getRecruiter())));
        inboxNotificationService.userIdForRecruiter(r.getRecruiter()).ifPresent(userId ->
                inboxNotificationService.notifyUser(
                        userId, type, r.getAppChat().getId(), r.getId(), null, preview, systemEvent,
                        ParticipantDisplayNames.student(r.getStudent())));
    }
}
