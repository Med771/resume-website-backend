package ru.ai.sin.logic.auth;

import jakarta.servlet.http.HttpServletRequest;
import ru.ai.sin.logic.auth.dto.AuthMeDTO;
import ru.ai.sin.logic.auth.dto.ChangePasswordReq;
import ru.ai.sin.logic.auth.dto.LoginRequest;
import ru.ai.sin.logic.auth.dto.TokenPair;

public interface AuthService {

    /** Вход на основной сайт (STUDENT / RECRUITER). */
    TokenPair login(LoginRequest request);

    /** Вход в админ-панель (только ADMIN). */
    TokenPair adminLogin(LoginRequest request);

    /** Обновление access-токена основного сайта. */
    String refresh(HttpServletRequest request);

    /** Обновление access-токена админ-панели. */
    String adminRefresh(HttpServletRequest request);

    AuthMeDTO getCurrentSession();

    void changePassword(ChangePasswordReq req);
}
