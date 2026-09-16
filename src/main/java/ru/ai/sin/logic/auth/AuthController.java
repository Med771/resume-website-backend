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
import ru.ai.sin.logic.recruiter.registration.RecruiterSelfRegistrationService;
import ru.ai.sin.logic.recruiter.registration.dto.RecruiterSelfRegistrationReq;
import ru.ai.sin.logic.registration.StudentRegistrationService;
import ru.ai.sin.logic.registration.dto.StudentAccountRegistrationReq;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Аутентификация основного сайта (STUDENT / RECRUITER)")
public class AuthController {

    private final AuthService authService;
    private final StudentRegistrationService studentRegistrationService;
    private final RecruiterSelfRegistrationService recruiterSelfRegistrationService;
    private final CookieHelper cookieHelper;

    @Operation(
            summary = "Регистрация работодателя",
            description = "Создаёт профиль рекрутера и пользователя со статусом PENDING_APPROVAL. Выдаёт cookie для входа в ЛК.")
    @PostMapping("/register-recruiter")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerRecruiter(
            @Valid @RequestBody RecruiterSelfRegistrationReq req,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        setAuthCookies(response, recruiterSelfRegistrationService.registerAndIssueTokens(req, httpRequest));
    }

    @Operation(
            summary = "Саморегистрация студента (учётная запись)",
            description = "Создаёт аккаунт STUDENT и черновик карточки (catalogVisible=false) после подтверждения телефона. "
                    + "Дозаполнение резюме — PATCH /student/me и CRUD /experience, /institution, /portfolio. Cookie как при входе.")
    @PostMapping("/register-student")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerStudent(
            @Valid @RequestBody StudentAccountRegistrationReq req,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        setAuthCookies(response, studentRegistrationService.registerAndIssueTokens(req, httpRequest));
    }

    @Operation(summary = "Вход на основной сайт", description = "STUDENT / RECRUITER. Администраторы — /auth/admin/login")
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void login(@RequestBody LoginRequest request, HttpServletResponse response) {
        setAuthCookies(response, authService.login(request));
    }

    @Operation(summary = "Обновить access-токен основного сайта")
    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void refresh(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = authService.refresh(request);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieHelper.createAccessTokenCookie(accessToken).toString());
    }

    @Operation(summary = "Выход из основного сайта")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletResponse response) {
        clearAuthCookies(response);
    }

    @Operation(summary = "Текущая сессия основного сайта")
    @GetMapping("/me")
    public AuthMeDTO me() {
        return authService.getCurrentSession();
    }

    @Operation(summary = "Сменить пароль текущего пользователя")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/change-password")
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
