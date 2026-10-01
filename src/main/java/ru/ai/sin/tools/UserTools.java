package ru.ai.sin.tools;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;

import java.util.Optional;

/**
 * Текущий пользователь из security-контекста и связанные сущности.
 */
@Component
@RequiredArgsConstructor
public class UserTools {

    private final UserRepo userRepo;
    private final SecurityHelper securityHelper;

    /**
     * Пользователь по JWT-логину с подгруженным рекрутером (без отдельной транзакции — в транзакции вызывающего метода).
     */
    public Optional<UserEnt> findCurrentUserFetchingRecruiter() {
        return securityHelper
                .getCurrentUsernameOptional()
                .flatMap(userRepo::findByUsernameFetchingRecruiter);
    }

    /** Пользователь с подгруженными рекрутером и студентом (для чатов и заявок). */
    public Optional<UserEnt> findCurrentUserFetchingLinks() {
        return securityHelper
                .getCurrentUsernameOptional()
                .flatMap(userRepo::findByUsernameFetchingLinks);
    }
}
