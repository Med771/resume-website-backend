package ru.ai.sin.logic.registration;

import jakarta.servlet.http.HttpServletRequest;
import ru.ai.sin.logic.auth.dto.TokenPair;
import ru.ai.sin.logic.registration.dto.StudentAccountRegistrationReq;

public interface StudentRegistrationService {

    /**
     * Создаёт пользователя STUDENT и черновик карточки ({@code catalogVisible=false}), выполняет аутентификацию.
     * Дозаполнение резюме — {@code PATCH /student/me} и CRUD {@code /experience}, {@code /institution}, {@code /portfolio}.
     */
    TokenPair registerAndIssueTokens(StudentAccountRegistrationReq req, HttpServletRequest httpRequest);
}
