package ru.ai.sin.logic.student;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.ai.sin.config.property.MailProperties;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.helper.ParticipantDisplayNames;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.student.dto.AdminStudentMailReq;
import ru.ai.sin.logic.student.dto.AdminStudentMailResult;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.logic.verification.VerificationOtpMailer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStudentMailService {

    static final int MAX_RECIPIENTS = 200;

    private final UserRepo userRepo;
    private final MailProperties mailProperties;
    private final VerificationOtpMailer verificationOtpMailer;
    private final SecurityHelper securityHelper;

    public AdminStudentMailResult send(AdminStudentMailReq req) {
        List<UUID> ids = distinctIds(req.studentIds());
        if (ids.isEmpty() && !req.effectiveAll()) {
            throw new BadRequestException("Укажите studentIds или all=true");
        }
        if (ids.size() > MAX_RECIPIENTS) {
            throw new BadRequestException("Не больше 200 получателей за один запрос");
        }

        List<UserEnt> loaded = ids.isEmpty()
                ? userRepo.findAllStudentUsers()
                : userRepo.findStudentsByStudentIds(ids);
        if (ids.isEmpty() && loaded.size() > MAX_RECIPIENTS) {
            throw new BadRequestException("Не больше 200 получателей за один запрос");
        }
        requireMailConfigured();

        String subject = req.subject().trim();
        String body = req.body().trim();
        Map<UUID, UserEnt> byStudentId = indexByStudentId(loaded);

        List<AdminStudentMailResult.Sent> sent = new ArrayList<>();
        List<AdminStudentMailResult.Skipped> skipped = new ArrayList<>();
        List<AdminStudentMailResult.Failed> failed = new ArrayList<>();
        Set<String> usedEmails = new HashSet<>();

        if (ids.isEmpty()) {
            for (UserEnt user : loaded) {
                deliver(user, subject, body, usedEmails, sent, skipped, failed);
            }
        } else {
            for (UUID studentId : ids) {
                UserEnt user = byStudentId.get(studentId);
                if (user == null) {
                    skipped.add(new AdminStudentMailResult.Skipped(studentId, null, null, "NOT_FOUND"));
                    continue;
                }
                deliver(user, subject, body, usedEmails, sent, skipped, failed);
            }
        }

        log.info(
                "Admin student mail by {}: subject={} sent={} skipped={} failed={}",
                securityHelper.getCurrentUsername(),
                subject,
                sent.size(),
                skipped.size(),
                failed.size());
        return new AdminStudentMailResult(List.copyOf(sent), List.copyOf(skipped), List.copyOf(failed));
    }

    private void deliver(
            UserEnt user,
            String subject,
            String body,
            Set<String> usedEmails,
            List<AdminStudentMailResult.Sent> sent,
            List<AdminStudentMailResult.Skipped> skipped,
            List<AdminStudentMailResult.Failed> failed
    ) {
        UUID studentId = user.getStudent().getId();
        String name = displayName(user);
        String email = studentEmail(user);
        if (email == null) {
            skipped.add(new AdminStudentMailResult.Skipped(studentId, name, null, "NO_EMAIL"));
            return;
        }
        if (!usedEmails.add(email.toLowerCase(Locale.ROOT))) {
            skipped.add(new AdminStudentMailResult.Skipped(studentId, name, email, "DUPLICATE"));
            return;
        }
        Optional<String> error = verificationOtpMailer.trySendPlain(email, subject, body);
        if (error.isEmpty()) {
            sent.add(new AdminStudentMailResult.Sent(studentId, name, email));
        } else {
            failed.add(new AdminStudentMailResult.Failed(studentId, name, email, error.get()));
        }
    }

    private void requireMailConfigured() {
        if (!mailProperties.isEnabled()) {
            throw new BadRequestException("Почта выключена");
        }
        if (mailProperties.getFrom() == null || mailProperties.getFrom().isBlank()) {
            throw new BadRequestException("Не задан адрес отправителя");
        }
    }

    private static Map<UUID, UserEnt> indexByStudentId(List<UserEnt> users) {
        Map<UUID, UserEnt> byStudentId = new LinkedHashMap<>();
        for (UserEnt user : users) {
            if (user.getStudent() != null && user.getStudent().getId() != null) {
                byStudentId.putIfAbsent(user.getStudent().getId(), user);
            }
        }
        return byStudentId;
    }

    private static List<UUID> distinctIds(List<UUID> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<UUID> unique = new LinkedHashSet<>();
        for (UUID id : studentIds) {
            if (id != null) {
                unique.add(id);
            }
        }
        return List.copyOf(unique);
    }

    private static String displayName(UserEnt user) {
        String name = ParticipantDisplayNames.student(user.getStudent());
        if (name != null && !name.isBlank()) {
            return name;
        }
        return user.getUsername();
    }

    private static String studentEmail(UserEnt user) {
        if (user.getRegistrationEmail() != null && !user.getRegistrationEmail().isBlank()) {
            return user.getRegistrationEmail().trim();
        }
        StudentEnt student = user.getStudent();
        if (student != null
                && student.getUserInformation() != null
                && student.getUserInformation().getEmail() != null
                && !student.getUserInformation().getEmail().isBlank()) {
            return student.getUserInformation().getEmail().trim();
        }
        return null;
    }
}
