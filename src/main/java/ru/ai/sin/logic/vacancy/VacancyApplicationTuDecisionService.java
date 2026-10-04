package ru.ai.sin.logic.vacancy;

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
import ru.ai.sin.models.enums.RoleEnum;
import ru.ai.sin.models.enums.UserInboxNotificationType;
import ru.ai.sin.models.enums.VacancyApplicationStatus;
import ru.ai.sin.helper.ParticipantDisplayNames;
import ru.ai.sin.logic.notification.UserInboxNotificationService;

import ru.ai.sin.tools.UserTools;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VacancyApplicationTuDecisionService {

    private final VacancyApplicationRepo vacancyApplicationRepo;
    private final UserTools userTools;
    private final ChatService chatService;
    private final AccountAccessHelper accountAccessHelper;
    private final UserInboxNotificationService inboxNotificationService;

    @Transactional
    public void decide(UUID applicationId, TuDecisionReq req) {
        accountAccessHelper.requireApprovedAccount();
        VacancyApplicationEnt app = vacancyApplicationRepo.findWithDetailsById(applicationId)
                .orElseThrow(() -> new NotFoundException("Отклик не найден: " + applicationId));
        if (app.getStatus() != VacancyApplicationStatus.ACCEPTED) {
            throw new BadRequestException("ТУ доступно только после принятия отклика");
        }
        UserEnt user = userTools.findCurrentUserFetchingLinks()
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (Boolean.FALSE.equals(req.accept())) {
            applyRejection(app, req, user);
            return;
        }
        if (req.accept() == null || !req.accept()) {
            throw new BadRequestException("Укажите accept: true или false");
        }

        if (user.getRole() == RoleEnum.STUDENT) {
            confirmAsStudent(app, user);
        } else if (user.getRole() == RoleEnum.RECRUITER) {
            confirmAsRecruiter(app, user);
        } else {
            throw new BadRequestException("Недоступно для вашей роли");
        }
    }

    private void confirmAsStudent(VacancyApplicationEnt app, UserEnt user) {
        if (user.getStudent() == null || !user.getStudent().getId().equals(app.getStudent().getId())) {
            throw new BadRequestException("Это не ваш отклик");
        }
        if (app.getStudentTuConfirmedAt() != null) {
            throw new BadRequestException("Вы уже подтвердили ТУ по этому отклику");
        }
        app.setStudentTuConfirmedAt(LocalDateTime.now());
        if (app.getRecruiterTuConfirmedAt() != null) {
            postSuccess(app);
        } else {
            postPartialStudentConfirmed(app);
        }
        vacancyApplicationRepo.save(app);
    }

    private void confirmAsRecruiter(VacancyApplicationEnt app, UserEnt user) {
        if (user.getRecruiter() == null
                || !user.getRecruiter().getId().equals(app.getVacancy().getRecruiter().getId())) {
            throw new BadRequestException("Это не ваша вакансия");
        }
        if (app.getRecruiterTuConfirmedAt() != null) {
            throw new BadRequestException("Вы уже подтвердили ТУ по этому отклику");
        }
        app.setRecruiterTuConfirmedAt(LocalDateTime.now());
        if (app.getStudentTuConfirmedAt() != null) {
            postSuccess(app);
        } else {
            postPartialRecruiterConfirmed(app);
        }
        vacancyApplicationRepo.save(app);
    }

    private void applyRejection(VacancyApplicationEnt app, TuDecisionReq req, UserEnt user) {
        if (user.getRole() != RoleEnum.STUDENT && user.getRole() != RoleEnum.RECRUITER) {
            throw new BadRequestException("Недоступно для вашей роли");
        }
        if (user.getRole() == RoleEnum.STUDENT) {
            if (user.getStudent() == null || !user.getStudent().getId().equals(app.getStudent().getId())) {
                throw new BadRequestException("Это не ваш отклик");
            }
        } else {
            if (user.getRecruiter() == null
                    || !user.getRecruiter().getId().equals(app.getVacancy().getRecruiter().getId())) {
                throw new BadRequestException("Это не ваша вакансия");
            }
        }
        AccountAccessHelper.validateRejection(req.reasonCode(), req.comment());
        app.setRejectionReasonCode(RejectionReasonCode.fromCode(req.reasonCode()).getCode());
        app.setRejectionComment(req.comment());
        app.setStatus(VacancyApplicationStatus.REJECTED);
        vacancyApplicationRepo.save(app);
        ChatEnt chat = app.getAppChat();
        if (chat != null) {
            String body = "Отказ по отклику: " + req.reasonCode();
            chatService.postSystemMessage(chat, ChatSystemEvent.TU_REJECTED, body);
            notifyBoth(app, UserInboxNotificationType.TU_REJECTED, body, ChatSystemEvent.TU_REJECTED);
        }
    }

    private void postPartialStudentConfirmed(VacancyApplicationEnt app) {
        ChatEnt chat = app.getAppChat();
        if (chat != null) {
            String body = "Студент подтвердил ТУ по отклику на «" + app.getVacancy().getTitle()
                    + "». Ожидается подтверждение работодателя.";
            chatService.postSystemMessage(chat, ChatSystemEvent.TU_STUDENT_CONFIRMED, body);
            inboxNotificationService.userIdForRecruiter(app.getVacancy().getRecruiter()).ifPresent(userId ->
                    inboxNotificationService.notifyUser(
                            userId, UserInboxNotificationType.TU_PARTIAL, chat.getId(), null, app.getId(),
                            body, ChatSystemEvent.TU_STUDENT_CONFIRMED,
                            ParticipantDisplayNames.student(app.getStudent())));
        }
    }

    private void postPartialRecruiterConfirmed(VacancyApplicationEnt app) {
        ChatEnt chat = app.getAppChat();
        if (chat != null) {
            String body = "Работодатель подтвердил ТУ по отклику на «" + app.getVacancy().getTitle()
                    + "». Ожидается подтверждение студента.";
            chatService.postSystemMessage(chat, ChatSystemEvent.TU_RECRUITER_CONFIRMED, body);
            inboxNotificationService.userIdForStudent(app.getStudent()).ifPresent(userId ->
                    inboxNotificationService.notifyUser(
                            userId, UserInboxNotificationType.TU_PARTIAL, chat.getId(), null, app.getId(),
                            body, ChatSystemEvent.TU_RECRUITER_CONFIRMED,
                            ParticipantDisplayNames.recruiter(app.getVacancy().getRecruiter())));
        }
    }

    private void postSuccess(VacancyApplicationEnt app) {
        ChatEnt chat = app.getAppChat();
        if (chat != null) {
            String body = "Обе стороны подтвердили ТУ по отклику на «" + app.getVacancy().getTitle() + "».";
            chatService.postSystemMessage(chat, ChatSystemEvent.TU_CONFIRMED, body);
            notifyBoth(app, UserInboxNotificationType.TU_CONFIRMED, body, ChatSystemEvent.TU_CONFIRMED);
        }
    }

    private void notifyBoth(
            VacancyApplicationEnt app,
            UserInboxNotificationType type,
            String preview,
            String systemEvent
    ) {
        ChatEnt chat = app.getAppChat();
        if (chat == null) {
            return;
        }
        inboxNotificationService.userIdForStudent(app.getStudent()).ifPresent(userId ->
                inboxNotificationService.notifyUser(
                        userId, type, chat.getId(), null, app.getId(), preview, systemEvent,
                        ParticipantDisplayNames.recruiter(app.getVacancy().getRecruiter())));
        inboxNotificationService.userIdForRecruiter(app.getVacancy().getRecruiter()).ifPresent(userId ->
                inboxNotificationService.notifyUser(
                        userId, type, chat.getId(), null, app.getId(), preview, systemEvent,
                        ParticipantDisplayNames.student(app.getStudent())));
    }
}
