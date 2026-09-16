package ru.ai.sin.logic.registration;

import jakarta.servlet.http.HttpServletRequest;
import ru.ai.sin.logic.auth.dto.TokenPair;
import ru.ai.sin.logic.registration.dto.StudentAccountRegistrationReq;

public interface StudentRegistrationService {

    /**
     * Создаёт пользователя STUDENT и черновик карточки ({@code catalogVisible=false}), выполняет аутентификацию.
     * Письмо с кодом уходит сразу; подтверждение — {@code POST /auth/confirm-email}.
     */
    TokenPair registerAndIssueTokens(StudentAccountRegistrationReq req, HttpServletRequest httpRequest);

    void confirmEmail(String code);

    void resendEmailConfirmation();
}
