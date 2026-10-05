package ru.ai.sin.logic.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.mockito.ArgumentCaptor;
import ru.ai.sin.config.property.JwtProperties;
import ru.ai.sin.config.property.MailProperties;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.ForbiddenException;
import ru.ai.sin.helper.AuthRoleGuard;
import ru.ai.sin.helper.JwtHelper;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.auth.dto.AuthMeDTO;
import ru.ai.sin.logic.auth.dto.ForgotPasswordReq;
import ru.ai.sin.logic.auth.dto.LoginRequest;
import ru.ai.sin.logic.auth.dto.ResetPasswordReq;
import ru.ai.sin.logic.auth.dto.TokenPair;
import ru.ai.sin.logic.registration.EmailOtpAttemptLimiter;
import ru.ai.sin.logic.registration.RegistrationPasswordPolicy;
import ru.ai.sin.logic.verification.VerificationOtpMailer;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import org.springframework.security.crypto.password.PasswordEncoder;

import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.RoleEnum;

import jakarta.servlet.http.Cookie;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtHelper jwtHelper;

    @Mock
    private SecurityHelper securityHelper;

    @Mock
    private AuthRoleGuard authRoleGuard;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UserRepo userRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RegistrationPasswordPolicy registrationPasswordPolicy;

    @Mock
    private EmailOtpAttemptLimiter emailOtpAttemptLimiter;

    @Mock
    private VerificationOtpMailer verificationOtpMailer;

    private MailProperties mailProperties;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        JwtProperties.CookieProperties cookie = new JwtProperties.CookieProperties();
        cookie.setRefreshTokenName("REFRESH_TOKEN");
        jwtProperties.setCookie(cookie);
        mailProperties = new MailProperties();
        mailProperties.setOtpTtlMinutes(15);
        mailProperties.setAllowDevConfirm(false);
        authService = new AuthServiceImpl(
                authenticationManager,
                jwtHelper,
                jwtProperties,
                securityHelper,
                authRoleGuard,
                userDetailsService,
                userRepo,
                passwordEncoder,
                registrationPasswordPolicy,
                emailOtpAttemptLimiter,
                mailProperties,
                verificationOtpMailer);
    }

    @Test
    void login_returnsTokenPairForStudent() {
        UserDetails userDetails = User.withUsername("alice").password("x").roles("STUDENT").build();
        stubAuthentication(userDetails);

        TokenPair pair = authService.login(new LoginRequest("alice", "secret"));

        assertThat(pair.accessToken()).isEqualTo("access-jwt");
        verify(authRoleGuard).requireFrontendUser(userDetails);
    }

    @Test
    void adminLogin_returnsTokenPairForAdmin() {
        UserDetails userDetails = User.withUsername("admin").password("x").roles("ADMIN").build();
        stubAuthentication(userDetails);

        TokenPair pair = authService.adminLogin(new LoginRequest("admin", "secret"));

        assertThat(pair.accessToken()).isEqualTo("access-jwt");
        verify(authRoleGuard).requireAdmin(userDetails);
    }

    @Test
    void adminLogin_rejectsNonAdmin() {
        UserDetails userDetails = User.withUsername("student").password("x").roles("STUDENT").build();
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        doThrow(new ForbiddenException("denied")).when(authRoleGuard).requireAdmin(userDetails);

        assertThatThrownBy(() -> authService.adminLogin(new LoginRequest("student", "secret")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void refresh_throwsWhenNoRefreshCookie() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(null);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_returnsNewAccessTokenForStudent() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("REFRESH_TOKEN", "rt-value")});
        when(jwtHelper.getUsernameFromRefreshToken("rt-value")).thenReturn("bob");
        UserDetails userDetails = User.withUsername("bob").password("x").roles("STUDENT").build();
        when(userDetailsService.loadUserByUsername("bob")).thenReturn(userDetails);
        when(jwtHelper.generateAccessToken("bob")).thenReturn("new-access");

        assertThat(authService.refresh(request)).isEqualTo("new-access");
        verify(authRoleGuard).requireFrontendUser(userDetails);
    }

    @Test
    void adminRefresh_returnsNewAccessTokenForAdmin() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("REFRESH_TOKEN", "rt-value")});
        when(jwtHelper.getUsernameFromRefreshToken("rt-value")).thenReturn("admin");
        UserDetails userDetails = User.withUsername("admin").password("x").roles("ADMIN").build();
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails);
        when(jwtHelper.generateAccessToken("admin")).thenReturn("new-access");

        assertThat(authService.adminRefresh(request)).isEqualTo("new-access");
        verify(authRoleGuard).requireAdmin(userDetails);
    }

    @Test
    void getCurrentSession_includesUserId() {
        UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "Alice", "alice", "hash");
        user.setId(userId);
        user.setAccountStatus(AccountStatus.APPROVED);

        when(securityHelper.getCurrentUsernameOptional()).thenReturn(Optional.of("alice"));
        when(securityHelper.getCurrentRoleOptional()).thenReturn(Optional.of("STUDENT"));
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));

        AuthMeDTO me = authService.getCurrentSession();

        assertThat(me.id()).isEqualTo(userId);
        assertThat(me.username()).isEqualTo("alice");
        assertThat(me.role()).isEqualTo("STUDENT");
        assertThat(me.emailVerified()).isFalse();
    }

    @Test
    void forgotPassword_unknownEmail_doesNotSend() {
        when(userRepo.findFrontendUsersByNormalizedEmail(eq("missing@b.c"), any())).thenReturn(List.of());

        authService.forgotPassword(new ForgotPasswordReq("missing@b.c"), requestWithIp());

        verify(verificationOtpMailer, never()).trySendPasswordReset(anyString(), anyString(), anyInt(), anyString());
        verify(userRepo, never()).save(any());
    }

    @Test
    void forgotPassword_admin_doesNotSend() {
        UserEnt admin = new UserEnt(RoleEnum.ADMIN, "Admin", "admin", "old-hash");
        when(userRepo.findFrontendUsersByNormalizedEmail(eq("admin@b.c"), any())).thenReturn(List.of(admin));

        authService.forgotPassword(new ForgotPasswordReq("admin@b.c"), requestWithIp());

        verify(verificationOtpMailer, never()).trySendPasswordReset(anyString(), anyString(), anyInt(), anyString());
        verify(userRepo, never()).save(any());
    }

    @Test
    void forgotPassword_twoAccounts_doesNotSend() {
        UserEnt first = new UserEnt(RoleEnum.STUDENT, "A", "a", "h1");
        UserEnt second = new UserEnt(RoleEnum.RECRUITER, "B", "b", "h2");
        when(userRepo.findFrontendUsersByNormalizedEmail(eq("shared@b.c"), any()))
                .thenReturn(List.of(first, second));

        authService.forgotPassword(new ForgotPasswordReq("shared@b.c"), requestWithIp());

        verify(verificationOtpMailer, never()).trySendPasswordReset(anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    void forgotPassword_sendsFourDigitsAndStoresHash() {
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "Alice", "alice", "old-hash");
        when(userRepo.findFrontendUsersByNormalizedEmail(eq("a@b.c"), any())).thenReturn(List.of(user));
        when(verificationOtpMailer.trySendPasswordReset(anyString(), anyString(), anyInt(), anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("reset-hash");

        authService.forgotPassword(new ForgotPasswordReq("A@b.c"), requestWithIp());

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(verificationOtpMailer).trySendPasswordReset(eq("a@b.c"), code.capture(), eq(15), eq("alice"));
        assertThat(code.getValue()).matches("\\d{4}");
        assertThat(user.getPasswordResetOtpHash()).isEqualTo("reset-hash");
        assertThat(user.getPasswordResetOtpExpiresAt()).isAfter(LocalDateTime.now());
        verify(userRepo).save(user);
    }

    @Test
    void forgotPassword_mailNotSent_doesNotStoreHash() {
        UserEnt user = new UserEnt(RoleEnum.RECRUITER, "Bob", "bob", "old-hash");
        when(userRepo.findFrontendUsersByNormalizedEmail(eq("bob@b.c"), any())).thenReturn(List.of(user));
        when(verificationOtpMailer.trySendPasswordReset(anyString(), anyString(), anyInt(), anyString())).thenReturn(false);

        authService.forgotPassword(new ForgotPasswordReq("bob@b.c"), requestWithIp());

        assertThat(user.getPasswordResetOtpHash()).isNull();
        verify(userRepo, never()).save(any());
    }

    @Test
    void resetPassword_success_replacesHashAndClearsCode() {
        UserEnt user = resetUser();
        when(userRepo.findFrontendUsersByNormalizedEmail(eq("a@b.c"), any())).thenReturn(List.of(user));
        when(passwordEncoder.matches("1234", "reset-hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123")).thenReturn("new-hash");

        authService.resetPassword(
                new ResetPasswordReq("a@b.c", "1234", "NewPassword123", "NewPassword123"),
                requestWithIp());

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.getPasswordResetOtpHash()).isNull();
        assertThat(user.getPasswordResetOtpExpiresAt()).isNull();
        verify(registrationPasswordPolicy).validate("NewPassword123");
        verify(userRepo).save(user);
    }

    @Test
    void resetPassword_wrongCode_keepsPassword() {
        UserEnt user = resetUser();
        when(userRepo.findFrontendUsersByNormalizedEmail(eq("a@b.c"), any())).thenReturn(List.of(user));
        when(passwordEncoder.matches("9999", "reset-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.resetPassword(
                new ResetPasswordReq("a@b.c", "9999", "NewPassword123", "NewPassword123"),
                requestWithIp()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Неверный или просроченный код");
        assertThat(user.getPasswordHash()).isEqualTo("old-hash");
    }

    @Test
    void resetPassword_expiredCode_sameError() {
        UserEnt user = resetUser();
        user.setPasswordResetOtpExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(userRepo.findFrontendUsersByNormalizedEmail(eq("a@b.c"), any())).thenReturn(List.of(user));

        assertThatThrownBy(() -> authService.resetPassword(
                new ResetPasswordReq("a@b.c", "1234", "NewPassword123", "NewPassword123"),
                requestWithIp()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Неверный или просроченный код");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void resetPassword_unknownOrAdmin_sameError() {
        when(userRepo.findFrontendUsersByNormalizedEmail(eq("admin@b.c"), any())).thenReturn(List.of());

        assertThatThrownBy(() -> authService.resetPassword(
                new ResetPasswordReq("admin@b.c", "1234", "NewPassword123", "NewPassword123"),
                requestWithIp()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Неверный или просроченный код");
    }

    @Test
    void resetPassword_repeatAfterSuccess_rejected() {
        UserEnt user = resetUser();
        when(userRepo.findFrontendUsersByNormalizedEmail(eq("a@b.c"), any())).thenReturn(List.of(user));
        when(passwordEncoder.matches("1234", "reset-hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123")).thenReturn("new-hash");
        ResetPasswordReq req = new ResetPasswordReq("a@b.c", "1234", "NewPassword123", "NewPassword123");

        authService.resetPassword(req, requestWithIp());

        assertThatThrownBy(() -> authService.resetPassword(req, requestWithIp()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Неверный или просроченный код");
    }

    private static UserEnt resetUser() {
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "Alice", "alice", "old-hash");
        user.setPasswordResetOtpHash("reset-hash");
        user.setPasswordResetOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        return user;
    }

    private static HttpServletRequest requestWithIp() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.8");
        return request;
    }

    private void stubAuthentication(UserDetails userDetails) {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtHelper.generateAccessToken(userDetails.getUsername())).thenReturn("access-jwt");
        when(jwtHelper.generateRefreshToken(userDetails.getUsername())).thenReturn("refresh-jwt");
    }
}
