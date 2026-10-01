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
import ru.ai.sin.config.property.JwtProperties;
import ru.ai.sin.exception.models.ForbiddenException;
import ru.ai.sin.helper.AuthRoleGuard;
import ru.ai.sin.helper.JwtHelper;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.auth.dto.AuthMeDTO;
import ru.ai.sin.logic.auth.dto.LoginRequest;
import ru.ai.sin.logic.auth.dto.TokenPair;
import ru.ai.sin.logic.registration.RegistrationPasswordPolicy;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import org.springframework.security.crypto.password.PasswordEncoder;

import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.RoleEnum;

import jakarta.servlet.http.Cookie;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        JwtProperties.CookieProperties cookie = new JwtProperties.CookieProperties();
        cookie.setRefreshTokenName("REFRESH_TOKEN");
        jwtProperties.setCookie(cookie);
        authService = new AuthServiceImpl(
                authenticationManager,
                jwtHelper,
                jwtProperties,
                securityHelper,
                authRoleGuard,
                userDetailsService,
                userRepo,
                passwordEncoder,
                registrationPasswordPolicy);
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

    private void stubAuthentication(UserDetails userDetails) {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtHelper.generateAccessToken(userDetails.getUsername())).thenReturn("access-jwt");
        when(jwtHelper.generateRefreshToken(userDetails.getUsername())).thenReturn("refresh-jwt");
    }
}
