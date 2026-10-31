package ru.ai.sin.logic.account;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.account.dto.AccountApprovalUserDTO;
import ru.ai.sin.logic.account.dto.AccountEmailVerifiedReq;
import ru.ai.sin.logic.account.dto.AccountRejectReq;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.skill.SkillEnt;
import ru.ai.sin.logic.skill.SkillOrder;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.student.StudentRepo;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.logic.verification.VerificationOtpMailer;
import ru.ai.sin.models.PageResponse;
import ru.ai.sin.models.enums.AccountRejectionReason;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.BusynessEnum;
import ru.ai.sin.models.enums.CourseEnum;
import ru.ai.sin.models.enums.RoleEnum;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountApprovalServiceImpl implements AccountApprovalService {

    private final UserRepo userRepo;
    private final StudentRepo studentRepo;
    private final SecurityHelper securityHelper;
    private final VerificationOtpMailer verificationOtpMailer;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AccountApprovalUserDTO> listPending(RoleEnum role, int page, int size) {
        PageRequest pr = PageRequest.of(page, size);
        Page<UserEnt> result = role == null
                ? userRepo.findByAccountStatus(AccountStatus.PENDING_APPROVAL, pr)
                : userRepo.findByAccountStatusAndRole(AccountStatus.PENDING_APPROVAL, role, pr);
        return new PageResponse<>(
                result.getContent().stream().map(this::toDto).toList(),
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    @Transactional
    public void approve(UUID userId) {
        UserEnt user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + userId));
        if (user.getAccountStatus() != AccountStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Аккаунт уже обработан");
        }
        if (user.getRole() == RoleEnum.STUDENT) {
            if (!user.isEmailVerified()) {
                throw new BadRequestException("Студент ещё не подтвердил почту");
            }
            StudentEnt studentProfile = user.getStudent();
            if (studentProfile == null) {
                throw new BadRequestException("Анкета неполная: нет анкеты");
            }
            List<String> problems = StudentApprovalCompleteness.problems(studentProfile);
            if (!problems.isEmpty()) {
                throw new BadRequestException("Анкета неполная: " + String.join(", ", problems));
            }
        }
        user.setAccountStatus(AccountStatus.APPROVED);
        userRepo.save(user);
        StudentEnt student = user.getStudent();
        if (student != null) {
            student.setCatalogVisible(true);
            student.setRejectionReason(null);
            student.setRejectionComment(null);
            studentRepo.save(student);
        }
        if (user.getRole() == RoleEnum.STUDENT) {
            String email = studentEmail(user);
            if (email != null) {
                verificationOtpMailer.trySendAccountApproved(email, user.getUsername());
            }
        }
        log.info("Account approved: userId={} by {}", userId, securityHelper.getCurrentUsername());
    }

    @Override
    @Transactional
    public void setEmailVerified(UUID userId, AccountEmailVerifiedReq body) {
        UserEnt user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + userId));
        if (user.getRole() != RoleEnum.STUDENT) {
            throw new BadRequestException("Подтверждение почты доступно только для студента");
        }
        user.setEmailVerified(body.emailVerified());
        user.setEmailOtpHash(null);
        user.setEmailOtpExpiresAt(null);
        userRepo.save(user);
        log.info("Student email verification set: userId={} emailVerified={} by {}",
                userId, body.emailVerified(), securityHelper.getCurrentUsername());
    }

    @Override
    @Transactional
    public void reject(UUID userId, AccountRejectReq body) {
        UserEnt user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + userId));
        if (!canReject(user)) {
            throw new BadRequestException("Аккаунт уже обработан");
        }
        if (user.getRole() == RoleEnum.STUDENT) {
            rejectStudent(user, body);
        } else {
            user.setAccountStatus(AccountStatus.REJECTED);
            userRepo.save(user);
        }
        log.info("Account rejected: userId={} by {} reason={} comment={}",
                userId,
                securityHelper.getCurrentUsername(),
                body != null ? body.reasonCode() : null,
                body != null ? body.comment() : null);
    }

    private static boolean canReject(UserEnt user) {
        if (user.getRole() == RoleEnum.STUDENT) {
            return user.getAccountStatus() == AccountStatus.PENDING_APPROVAL
                    || user.getAccountStatus() == AccountStatus.APPROVED;
        }
        return user.getAccountStatus() == AccountStatus.PENDING_APPROVAL;
    }

    private void rejectStudent(UserEnt user, AccountRejectReq body) {
        AccountRejectionReason reason = parseRejectionReason(body);
        String comment = normalizeComment(body != null ? body.comment() : null);
        if (reason == AccountRejectionReason.OTHER && comment == null) {
            throw new BadRequestException("Укажите комментарий");
        }
        user.setAccountStatus(AccountStatus.REJECTED);
        userRepo.save(user);
        StudentEnt student = user.getStudent();
        if (student != null) {
            student.setRejectionReason(reason.getCode());
            student.setRejectionComment(comment);
            student.setCatalogVisible(false);
            studentRepo.save(student);
        }
        String email = studentEmail(user);
        if (email == null) {
            log.warn("Account rejection email skipped: no email, userId={}", user.getId());
            return;
        }
        verificationOtpMailer.trySendAccountRejected(
                email,
                user.getUsername(),
                reason.formulation(comment),
                comment);
    }

    private static AccountRejectionReason parseRejectionReason(AccountRejectReq body) {
        try {
            return AccountRejectionReason.fromCode(body != null ? body.reasonCode() : null);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }

    private static String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        return comment.trim();
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

    private AccountApprovalUserDTO toDto(UserEnt u) {
        String email = null;
        String phone = null;
        String telegram = null;
        String companyName = null;
        String city = null;
        String firstName = null;
        String lastName = null;
        String speciality = null;
        CourseEnum course = null;
        BusynessEnum busyness = null;
        String bio = null;
        Integer profileTextScore = null;
        List<String> skills = List.of();
        LocalDateTime createdAt = null;

        RecruiterEnt recruiter = u.getRecruiter();
        if (recruiter != null) {
            companyName = recruiter.getCompanyName();
            if (recruiter.getUserInformation() != null) {
                firstName = recruiter.getUserInformation().getFirstName();
                lastName = recruiter.getUserInformation().getLastName();
                email = recruiter.getUserInformation().getEmail();
            }
            if (recruiter.getContactInformation() != null) {
                phone = recruiter.getContactInformation().getPhoneNumber();
                telegram = recruiter.getContactInformation().getTelegramUsername();
            }
            if (recruiter.getTimestamps() != null) {
                createdAt = recruiter.getTimestamps().getCreatedAt();
            }
        }

        StudentEnt student = u.getStudent();
        if (student != null) {
            city = student.getCity();
            bio = student.getBio();
            course = student.getCourse();
            busyness = student.getBusyness();
            profileTextScore = student.getProfileTextScore();
            if (student.getUserInformation() != null) {
                if (firstName == null) {
                    firstName = student.getUserInformation().getFirstName();
                }
                if (lastName == null) {
                    lastName = student.getUserInformation().getLastName();
                }
                if (email == null) {
                    email = student.getUserInformation().getEmail();
                }
            }
            if (student.getContactInformation() != null) {
                if (phone == null) {
                    phone = student.getContactInformation().getPhoneNumber();
                }
                if (telegram == null) {
                    telegram = student.getContactInformation().getTelegramUsername();
                }
            }
            if (student.getSpeciality() != null) {
                speciality = student.getSpeciality().getName();
            }
            if (student.getSkills() != null) {
                skills = student.getSkills().stream()
                        .sorted(SkillOrder.byCreatedAtThenId())
                        .map(SkillEnt::getName)
                        .filter(Objects::nonNull)
                        .limit(16)
                        .toList();
            }
            if (createdAt == null && student.getTimestamps() != null) {
                createdAt = student.getTimestamps().getCreatedAt();
            }
        }

        return new AccountApprovalUserDTO(
                u.getId(),
                u.getUsername(),
                u.getName(),
                u.getRole(),
                u.getAccountStatus(),
                student != null ? student.getId() : null,
                recruiter != null ? recruiter.getId() : null,
                createdAt,
                u.isPhoneVerified(),
                u.isEmailVerified(),
                email,
                phone,
                telegram,
                companyName,
                city,
                firstName,
                lastName,
                speciality,
                course,
                busyness,
                bio,
                profileTextScore,
                skills
        );
    }
}
