package ru.ai.sin.logic.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import ru.ai.sin.logic.notification.dto.UserInboxNotificationDTO;
import ru.ai.sin.logic.notification.event.UserInboxNotificationEvent;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.models.enums.UserInboxNotificationType;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserInboxNotificationService {

    private final ApplicationEventPublisher eventPublisher;
    private final UserRepo userRepo;

    public void notifyUser(
            UUID recipientUserId,
            UserInboxNotificationType type,
            UUID chatId,
            Long requestId,
            UUID applicationId,
            String preview,
            String systemEvent,
            String counterpartyName
    ) {
        if (recipientUserId == null) {
            return;
        }
        var dto = new UserInboxNotificationDTO(
                type,
                chatId,
                requestId,
                applicationId,
                preview,
                systemEvent,
                LocalDateTime.now(),
                counterpartyName
        );
        eventPublisher.publishEvent(new UserInboxNotificationEvent(recipientUserId, dto));
    }

    public Optional<UUID> userIdForStudent(StudentEnt student) {
        if (student == null) {
            return Optional.empty();
        }
        return userRepo.findByStudent_Id(student.getId()).map(u -> u.getId());
    }

    public Optional<UUID> userIdForRecruiter(RecruiterEnt recruiter) {
        if (recruiter == null) {
            return Optional.empty();
        }
        return userRepo.findByRecruiter_Id(recruiter.getId()).map(u -> u.getId());
    }
}
