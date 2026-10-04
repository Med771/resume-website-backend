package ru.ai.sin.logic.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.ai.sin.helper.CookieHelper;
import ru.ai.sin.logic.auth.dto.AuthMeDTO;
import ru.ai.sin.logic.auth.dto.ChangePasswordReq;
import ru.ai.sin.logic.auth.dto.LoginRequest;
import ru.ai.sin.logic.auth.dto.TokenPair;

@RestController
@RequestMapping("/auth/admin")
@RequiredArgsConstructor
@Tag(name = "AdminAuth", description = "Аутентификация админ-панели (только роль ADMIN)")
public class AdminAuthController {

    private final AuthService authService;
    private final CookieHelper cookieHelper;

    @Operation(summary = "Вход в админ-панель", description = "Выдаёт cookie только пользователям с ролью ADMIN")
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void login(@RequestBody LoginRequest request, HttpServletResponse response) {
        setAuthCookies(response, authService.adminLogin(request));
    }

    @Operation(summary = "Обновить access-токен админ-сессии")
    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void refresh(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = authService.adminRefresh(request);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieHelper.createAccessTokenCookie(accessToken).toString());
    }

    @Operation(summary = "Текущая сессия админ-панели")
    @GetMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    public AuthMeDTO me() {
        return authService.getCurrentSession();
    }

    @Operation(summary = "Выход из админ-панели")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletResponse response) {
        clearAuthCookies(response);
    }

    @Operation(summary = "Сменить пароль администратора")
    @PostMapping("/change-password")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordReq req) {
        authService.changePassword(req);
    }

    private void setAuthCookies(HttpServletResponse response, TokenPair tokens) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieHelper.createAccessTokenCookie(tokens.accessToken()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieHelper.createRefreshTokenCookie(tokens.refreshToken()).toString());
    }

    private void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieHelper.clearAccessTokenCookie().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieHelper.clearRefreshTokenCookie().toString());
    }
}
