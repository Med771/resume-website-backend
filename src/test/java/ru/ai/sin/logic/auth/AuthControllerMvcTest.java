package ru.ai.sin.logic.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.ai.sin.config.MethodSecurityTestConfig;
import ru.ai.sin.filter.JwtCookieAuthenticationFilter;
import ru.ai.sin.helper.CookieHelper;
import ru.ai.sin.logic.auth.dto.LoginRequest;
import ru.ai.sin.logic.auth.dto.TokenPair;
import ru.ai.sin.logic.recruiter.registration.RecruiterSelfRegistrationService;
import ru.ai.sin.logic.registration.StudentRegistrationService;

import jakarta.servlet.http.HttpServletRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class AuthControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private StudentRegistrationService studentRegistrationService;

    @MockitoBean
    private RecruiterSelfRegistrationService recruiterSelfRegistrationService;

    @MockitoBean
    private CookieHelper cookieHelper;

    @MockitoBean
    private JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;

    @Test
    void login_noContentAndSetsCookies() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new TokenPair("access-jwt", "refresh-jwt"));
        when(cookieHelper.createAccessTokenCookie("access-jwt"))
                .thenReturn(ResponseCookie.from("ACCESS_TOKEN", "access-jwt").path("/").build());
        when(cookieHelper.createRefreshTokenCookie("refresh-jwt"))
                .thenReturn(ResponseCookie.from("REFRESH_TOKEN", "refresh-jwt").path("/").build());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"u\",\"password\":\"p\"}"))
                .andExpect(status().isNoContent())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void refresh_noContent() throws Exception {
        when(authService.refresh(any(HttpServletRequest.class))).thenReturn("new-access");
        when(cookieHelper.createAccessTokenCookie("new-access"))
                .thenReturn(ResponseCookie.from("ACCESS_TOKEN", "new-access").path("/").build());

        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isNoContent());

        verify(authService).refresh(any(HttpServletRequest.class));
    }

    @Test
    void logout_noContent() throws Exception {
        when(cookieHelper.clearAccessTokenCookie())
                .thenReturn(ResponseCookie.from("ACCESS_TOKEN", "").path("/").maxAge(0).build());
        when(cookieHelper.clearRefreshTokenCookie())
                .thenReturn(ResponseCookie.from("REFRESH_TOKEN", "").path("/").maxAge(0).build());

        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    void registerStudent_noVerificationId_noContentAndSetsCookies() throws Exception {
        when(studentRegistrationService.registerAndIssueTokens(any(), any(HttpServletRequest.class)))
                .thenReturn(new TokenPair("access-jwt", "refresh-jwt"));
        when(cookieHelper.createAccessTokenCookie("access-jwt"))
                .thenReturn(ResponseCookie.from("ACCESS_TOKEN", "access-jwt").path("/").build());
        when(cookieHelper.createRefreshTokenCookie("refresh-jwt"))
                .thenReturn(ResponseCookie.from("REFRESH_TOKEN", "refresh-jwt").path("/").build());

        mockMvc.perform(post("/auth/register-student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "newuser_x",
                                  "password": "SecurePass123",
                                  "passwordConfirm": "SecurePass123",
                                  "email": "ivan@test.local",
                                  "phoneNumber": "+79990001122",
                                  "phoneVerificationId": "11111111-1111-1111-1111-111111111111"
                                }
                                """))
                .andExpect(status().isNoContent())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));

        verify(studentRegistrationService).registerAndIssueTokens(any(), any(HttpServletRequest.class));
    }

    @Test
    void registerStudent_missingEmail_badRequest() throws Exception {
        mockMvc.perform(post("/auth/register-student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "newuser_x",
                                  "password": "SecurePass123",
                                  "passwordConfirm": "SecurePass123",
                                  "phoneNumber": "+79990001122"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(studentRegistrationService, never()).registerAndIssueTokens(any(), any());
    }

    @Test
    void confirmEmail_unauthorizedWithoutSession() throws Exception {
        mockMvc.perform(post("/auth/confirm-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"123456\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resendEmailConfirmation_unauthorizedWithoutSession() throws Exception {
        mockMvc.perform(post("/auth/resend-email-confirmation"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void confirmEmail_student_noContent() throws Exception {
        mockMvc.perform(post("/auth/confirm-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"123456\"}")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(studentRegistrationService).confirmEmail("123456");
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void resendEmailConfirmation_student_noContent() throws Exception {
        mockMvc.perform(post("/auth/resend-email-confirmation").with(csrf()))
                .andExpect(status().isNoContent());

        verify(studentRegistrationService).resendEmailConfirmation();
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void confirmEmail_recruiter_forbidden() throws Exception {
        mockMvc.perform(post("/auth/confirm-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"123456\"}")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(studentRegistrationService, never()).confirmEmail(any());
    }
}
