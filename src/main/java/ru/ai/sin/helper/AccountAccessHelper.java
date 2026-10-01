package ru.ai.sin.helper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.ForbiddenException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.RoleEnum;
import ru.ai.sin.tools.StudentTools;
import ru.ai.sin.tools.UserTools;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccountAccessHelper {

    private final SecurityHelper securityHelper;
    private final UserRepo userRepo;
    private final UserTools userTools;
    private final StudentTools studentTools;

    public UserEnt requireCurrentUser() {
        String username = securityHelper.getCurrentUsername();
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new ForbiddenException("Пользователь не найден"));
    }

    public void requireApprovedAccount() {
        UserEnt user = requireCurrentUser();
        if (user.getRole() == RoleEnum.ADMIN) {
            return;
        }
        if (user.getAccountStatus() != AccountStatus.APPROVED) {
            throw new ForbiddenException("Аккаунт ожидает одобрения администратором");
        }
    }

    public boolean isApprovedOrAdmin(UserEnt user) {
        if (user == null) {
            return false;
        }
        return user.getRole() == RoleEnum.ADMIN || user.getAccountStatus() == AccountStatus.APPROVED;
    }

    public boolean treatsAsAnonymousForCatalog(UserEnt user) {
        if (user == null) {
            return true;
        }
        if (user.getRole() == RoleEnum.ADMIN) {
            return false;
        }
        return user.getAccountStatus() != AccountStatus.APPROVED;
    }

    /** null if not authenticated */
    public UserEnt requireCurrentUserOptional() {
        try {
            String username = securityHelper.getCurrentUsernameOptional().orElse(null);
            if (username == null) {
                return null;
            }
            return userRepo.findByUsername(username).orElse(null);
        } catch (Exception ex) {
            return null;
        }
    }

    public void requireRecruiterOwnsProfile(UUID recruiterId) {
        UserEnt user = requireCurrentUser();
        if (user.getRole() == RoleEnum.ADMIN) {
            return;
        }
        if (user.getRole() != RoleEnum.RECRUITER || user.getRecruiter() == null
                || !user.getRecruiter().getId().equals(recruiterId)) {
            throw new ForbiddenException("Нет доступа к профилю рекрутера");
        }
    }

    public void requireStudentOwnsProfile(UUID studentId) {
        UserEnt user = requireCurrentUser();
        if (user.getRole() == RoleEnum.ADMIN) {
            return;
        }
        if (user.getRole() != RoleEnum.STUDENT || user.getStudent() == null
                || !user.getStudent().getId().equals(studentId)) {
            throw new ForbiddenException("Нет доступа к профилю студента");
        }
    }

    /** Админ — да. Студент — только своя карточка. Рекрутер — 403. */
    public void requireStudentCanMutateResume(UUID studentId) {
        requireStudentOwnsProfile(studentId);
    }

    /**
     * Id карточки текущего студента. Админу нужно передать {@code studentId} в теле запроса.
     */
    public UUID requireCurrentStudentId() {
        UserEnt user = userTools.findCurrentUserFetchingLinks()
                .orElseThrow(() -> new ForbiddenException("Пользователь не найден"));
        if (user.getRole() == RoleEnum.ADMIN) {
            throw new BadRequestException("Укажите studentId");
        }
        if (user.getRole() != RoleEnum.STUDENT || user.getStudent() == null) {
            throw new ForbiddenException("К аккаунту не привязана карточка студента");
        }
        return user.getStudent().getId();
    }

    /**
     * STUDENT — всегда своя карточка (поле в теле игнорируется).
     * ADMIN — обязательный {@code requestedStudentId}.
     */
    public UUID resolveStudentIdForResumeMutation(UUID requestedStudentId) {
        UserEnt user = userTools.findCurrentUserFetchingLinks()
                .orElseThrow(() -> new ForbiddenException("Пользователь не найден"));
        if (user.getRole() == RoleEnum.ADMIN) {
            if (requestedStudentId == null) {
                throw new BadRequestException("Укажите studentId");
            }
            return requestedStudentId;
        }
        if (user.getRole() != RoleEnum.STUDENT || user.getStudent() == null) {
            throw new ForbiddenException("К аккаунту не привязана карточка студента");
        }
        return user.getStudent().getId();
    }

    /**
     * Те же правила видимости, что у {@code StudentServiceImpl#getById}: свой профиль всегда;
     * чужой — только при {@code catalogVisible} и одобренном аккаунте (кроме ADMIN).
     */
    public void requireCanReadStudentResumeDetails(UUID studentId) {
        if (studentId == null) {
            UserEnt user = requireCurrentUser();
            if (user.getRole() == RoleEnum.ADMIN || user.getRole() == RoleEnum.RECRUITER) {
                requireApprovedAccount();
                return;
            }
            throw new ForbiddenException("Нет доступа к данным резюме");
        }

        boolean isOwnProfile = userTools.findCurrentUserFetchingLinks()
                .filter(u -> u.getRole() == RoleEnum.STUDENT && u.getStudent() != null)
                .map(u -> u.getStudent().getId().equals(studentId))
                .orElse(false);
        if (!isOwnProfile) {
            requireApprovedAccount();
        }

        StudentEnt studentEnt = studentTools.getStudentOrThrow(studentId);
        if (!studentEnt.isCatalogVisible() && !securityHelper.isCurrentUserAdmin() && !isOwnProfile) {
            throw new NotFoundException("Failed to find student by id " + studentId);
        }
    }

    public static void validateRejection(String reasonCode, String comment) {
        if (reasonCode == null || reasonCode.isBlank()) {
            throw new BadRequestException("Укажите причину отказа");
        }
    }
}
