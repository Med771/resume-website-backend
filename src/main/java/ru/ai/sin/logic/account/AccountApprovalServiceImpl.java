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
import ru.ai.sin.logic.account.dto.AccountRejectReq;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.skill.SkillEnt;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.student.StudentRepo;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.models.PageResponse;
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
        if (user.getRole() == RoleEnum.STUDENT && !user.isEmailVerified()) {
            throw new BadRequestException("Студент ещё не подтвердил почту");
        }
        user.setAccountStatus(AccountStatus.APPROVED);
        userRepo.save(user);
        StudentEnt student = user.getStudent();
        if (student != null) {
            student.setCatalogVisible(true);
            studentRepo.save(student);
        }
        log.info("Account approved: userId={} by {}", userId, securityHelper.getCurrentUsername());
    }

    @Override
    @Transactional
    public void reject(UUID userId, AccountRejectReq body) {
        UserEnt user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + userId));
        if (user.getAccountStatus() != AccountStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Аккаунт уже обработан");
        }
        user.setAccountStatus(AccountStatus.REJECTED);
        userRepo.save(user);
        log.info("Account rejected: userId={} by {} comment={}", userId, securityHelper.getCurrentUsername(),
                body != null ? body.comment() : null);
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
