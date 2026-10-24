package ru.ai.sin.logic.account;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.RoleEnum;

@Service
@RequiredArgsConstructor
public class StudentReviewState {

    private final UserRepo userRepo;
    private final SecurityHelper securityHelper;

    /**
     * Правка своей анкеты студентом: одобренная или отклонённая карточка снова ждёт проверки и скрывается.
     * Пока статус уже {@link AccountStatus#PENDING_APPROVAL}, повторный вызов ничего не меняет.
     * Правка администратора статус не трогает.
     */
    @Transactional
    public void onStudentSelfEdit(StudentEnt student) {
        if (student == null || student.getId() == null || !isCurrentStudent()) {
            return;
        }
        UserEnt user = userRepo.findByStudent_Id(student.getId()).orElse(null);
        if (user == null) {
            return;
        }
        AccountStatus status = user.getAccountStatus();
        if (status != AccountStatus.APPROVED && status != AccountStatus.REJECTED) {
            return;
        }
        user.setAccountStatus(AccountStatus.PENDING_APPROVAL);
        userRepo.save(user);
        student.setCatalogVisible(false);
        student.setRejectionReason(null);
        student.setRejectionComment(null);
    }

    /**
     * {@code false} сохраняется всегда. {@code true} — только у аккаунта {@link AccountStatus#APPROVED}.
     */
    @Transactional(readOnly = true)
    public boolean catalogVisible(StudentEnt student, boolean requested) {
        if (!requested || student == null || student.getId() == null) {
            return false;
        }
        return userRepo.findByStudent_Id(student.getId())
                .map(user -> user.getAccountStatus() == AccountStatus.APPROVED)
                .orElse(false);
    }

    private boolean isCurrentStudent() {
        return securityHelper.getCurrentRoleOptional()
                .filter(role -> RoleEnum.STUDENT.getRole().equals(role))
                .isPresent();
    }
}
