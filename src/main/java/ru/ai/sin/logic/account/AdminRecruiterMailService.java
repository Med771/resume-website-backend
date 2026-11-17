package ru.ai.sin.logic.account;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.ai.sin.config.property.MailProperties;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.helper.ParticipantDisplayNames;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.account.dto.AdminRecruiterMailReq;
import ru.ai.sin.logic.account.dto.AdminRecruiterMailResult;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
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
public class AdminRecruiterMailService {

    static final int MAX_RECIPIENTS = 200;

    private final UserRepo userRepo;
    private final MailProperties mailProperties;
    private final VerificationOtpMailer verificationOtpMailer;
    private final SecurityHelper securityHelper;

    public AdminRecruiterMailResult send(AdminRecruiterMailReq req) {
        List<UUID> ids = distinctIds(req.recruiterIds());
        if (ids.isEmpty() && !req.effectiveAll()) {
            throw new BadRequestException("Укажите recruiterIds или all=true");
        }
        if (ids.size() > MAX_RECIPIENTS) {
            throw new BadRequestException("Не больше 200 получателей за один запрос");
        }

        List<UserEnt> loaded = ids.isEmpty()
                ? userRepo.findAllRecruiterUsers()
                : userRepo.findRecruitersByRecruiterIds(ids);
        if (ids.isEmpty() && loaded.size() > MAX_RECIPIENTS) {
            throw new BadRequestException("Не больше 200 получателей за один запрос");
        }
        requireMailConfigured();

        String subject = req.subject().trim();
        String body = req.body().trim();
        Map<UUID, UserEnt> byRecruiterId = indexByRecruiterId(loaded);

        List<AdminRecruiterMailResult.Sent> sent = new ArrayList<>();
        List<AdminRecruiterMailResult.Skipped> skipped = new ArrayList<>();
        List<AdminRecruiterMailResult.Failed> failed = new ArrayList<>();
        Set<String> usedEmails = new HashSet<>();

        if (ids.isEmpty()) {
            for (UserEnt user : loaded) {
                deliver(user, subject, body, usedEmails, sent, skipped, failed);
            }
        } else {
            for (UUID recruiterId : ids) {
                UserEnt user = byRecruiterId.get(recruiterId);
                if (user == null) {
                    skipped.add(new AdminRecruiterMailResult.Skipped(recruiterId, null, null, "NOT_FOUND"));
                    continue;
                }
                deliver(user, subject, body, usedEmails, sent, skipped, failed);
            }
        }

        log.info(
                "Admin recruiter mail by {}: subject={} sent={} skipped={} failed={}",
                securityHelper.getCurrentUsername(),
                subject,
                sent.size(),
                skipped.size(),
                failed.size());
        return new AdminRecruiterMailResult(List.copyOf(sent), List.copyOf(skipped), List.copyOf(failed));
    }

    private void deliver(
            UserEnt user,
            String subject,
            String body,
            Set<String> usedEmails,
            List<AdminRecruiterMailResult.Sent> sent,
            List<AdminRecruiterMailResult.Skipped> skipped,
            List<AdminRecruiterMailResult.Failed> failed
    ) {
        UUID recruiterId = user.getRecruiter().getId();
        String name = displayName(user);
        String email = recruiterEmail(user);
        if (email == null) {
            skipped.add(new AdminRecruiterMailResult.Skipped(recruiterId, name, null, "NO_EMAIL"));
            return;
        }
        if (!usedEmails.add(email.toLowerCase(Locale.ROOT))) {
            skipped.add(new AdminRecruiterMailResult.Skipped(recruiterId, name, email, "DUPLICATE"));
            return;
        }
        String personalSubject = applyPlaceholders(subject, user, email, name);
        String personalBody = applyPlaceholders(body, user, email, name);
        Optional<String> error = verificationOtpMailer.trySendPlain(email, personalSubject, personalBody);
        if (error.isEmpty()) {
            sent.add(new AdminRecruiterMailResult.Sent(recruiterId, name, email));
        } else {
            failed.add(new AdminRecruiterMailResult.Failed(recruiterId, name, email, error.get()));
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

    private static Map<UUID, UserEnt> indexByRecruiterId(List<UserEnt> users) {
        Map<UUID, UserEnt> byRecruiterId = new LinkedHashMap<>();
        for (UserEnt user : users) {
            if (user.getRecruiter() != null && user.getRecruiter().getId() != null) {
                byRecruiterId.putIfAbsent(user.getRecruiter().getId(), user);
            }
        }
        return byRecruiterId;
    }

    private static List<UUID> distinctIds(List<UUID> recruiterIds) {
        if (recruiterIds == null || recruiterIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<UUID> unique = new LinkedHashSet<>();
        for (UUID id : recruiterIds) {
            if (id != null) {
                unique.add(id);
            }
        }
        return List.copyOf(unique);
    }

    private static String applyPlaceholders(String template, UserEnt user, String email, String name) {
        RecruiterEnt recruiter = user.getRecruiter();
        String firstName = "";
        String lastName = "";
        if (recruiter.getUserInformation() != null) {
            firstName = empty(recruiter.getUserInformation().getFirstName());
            lastName = empty(recruiter.getUserInformation().getLastName());
        }
        return template
                .replace("{{recruiterId}}", recruiter.getId().toString())
                .replace("{{firstName}}", firstName)
                .replace("{{lastName}}", lastName)
                .replace("{{name}}", name)
                .replace("{{username}}", empty(user.getUsername()))
                .replace("{{email}}", email)
                .replace("{{companyName}}", empty(recruiter.getCompanyName()));
    }

    private static String empty(String value) {
        return value == null ? "" : value;
    }

    private static String displayName(UserEnt user) {
        String name = ParticipantDisplayNames.recruiter(user.getRecruiter());
        if (name != null && !name.isBlank()) {
            return name;
        }
        return user.getUsername();
    }

    private static String recruiterEmail(UserEnt user) {
        if (user.getRegistrationEmail() != null && !user.getRegistrationEmail().isBlank()) {
            return user.getRegistrationEmail().trim();
        }
        RecruiterEnt recruiter = user.getRecruiter();
        if (recruiter != null
                && recruiter.getUserInformation() != null
                && recruiter.getUserInformation().getEmail() != null
                && !recruiter.getUserInformation().getEmail().isBlank()) {
            return recruiter.getUserInformation().getEmail().trim();
        }
        return null;
    }
}
