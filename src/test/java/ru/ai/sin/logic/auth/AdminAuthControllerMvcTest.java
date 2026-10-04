package ru.ai.sin.logic.auth;

import jakarta.servlet.http.HttpServletRequest;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class AdminAuthControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CookieHelper cookieHelper;

    @MockitoBean
    private JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;

    @Test
    void adminLogin_setsCookies() throws Exception {
        when(authService.adminLogin(any(LoginRequest.class)))
                .thenReturn(new TokenPair("access-jwt", "refresh-jwt"));
        when(cookieHelper.createAccessTokenCookie("access-jwt"))
                .thenReturn(ResponseCookie.from("ACCESS_TOKEN", "access-jwt").path("/").build());
        when(cookieHelper.createRefreshTokenCookie("refresh-jwt"))
                .thenReturn(ResponseCookie.from("REFRESH_TOKEN", "refresh-jwt").path("/").build());

        mockMvc.perform(post("/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"secret\"}"))
                .andExpect(status().isNoContent())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));

        verify(authService).adminLogin(any(LoginRequest.class));
    }

    @Test
    void adminRefresh_noContent() throws Exception {
        when(authService.adminRefresh(any(HttpServletRequest.class))).thenReturn("new-access");
        when(cookieHelper.createAccessTokenCookie("new-access"))
                .thenReturn(ResponseCookie.from("ACCESS_TOKEN", "new-access").path("/").build());

        mockMvc.perform(post("/auth/admin/refresh"))
                .andExpect(status().isNoContent());

        verify(authService).adminRefresh(any(HttpServletRequest.class));
    }

    @Test
    void adminMe_unauthorized() throws Exception {
        mockMvc.perform(get("/auth/admin/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminMe_ok() throws Exception {
        when(authService.getCurrentSession()).thenReturn(
                new ru.ai.sin.logic.auth.dto.AuthMeDTO(
                        java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "admin", "ADMIN", "APPROVED", true, false));

        mockMvc.perform(get("/auth/admin/me").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void adminMe_forbidden() throws Exception {
        mockMvc.perform(get("/auth/admin/me").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
