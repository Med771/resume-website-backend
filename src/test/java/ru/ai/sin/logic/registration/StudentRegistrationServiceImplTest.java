package ru.ai.sin.logic.registration;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.ai.sin.config.property.MailProperties;
import ru.ai.sin.config.property.RegistrationProperties;
import ru.ai.sin.config.property.UserProperties;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.ForbiddenException;
import ru.ai.sin.exception.models.TooManyRequestsException;
import ru.ai.sin.helper.JwtHelper;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.registration.dto.StudentAccountRegistrationReq;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.student.StudentRepo;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.logic.verification.VerificationOtpMailer;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.RoleEnum;

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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentRegistrationServiceImplTest {

    @Mock
    private RegistrationIpRateLimiter registrationIpRateLimiter;
    @Mock
    private EmailOtpAttemptLimiter emailOtpAttemptLimiter;
    @Mock
    private RegistrationPasswordPolicy passwordPolicy;
    @Mock
    private RegistrationProperties registrationProperties;
    @Mock
    private UserProperties userProperties;
    @Mock
    private MailProperties mailProperties;
    @Mock
    private VerificationOtpMailer verificationOtpMailer;
    @Mock
    private UserRepo userRepo;
    @Mock
    private StudentRepo studentRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtHelper jwtHelper;
    @Mock
    private SecurityHelper securityHelper;
    @Mock
    private HttpServletRequest httpRequest;

    @Captor
    private ArgumentCaptor<StudentEnt> studentCaptor;

    @Captor
    private ArgumentCaptor<UserEnt> userCaptor;

    private StudentRegistrationServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(registrationProperties.isReservedUsername(anyString(), any())).thenReturn(false);
        lenient().when(userProperties.getLogins()).thenReturn(List.of());
        lenient().when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(mailProperties.getOtpTtlMinutes()).thenReturn(15);
        lenient().when(mailProperties.isAllowDevConfirm()).thenReturn(true);
        lenient().when(verificationOtpMailer.trySendOtp(anyString(), anyString(), anyInt())).thenReturn(true);
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("hash");
        lenient().when(studentRepo.existsByNormalizedEmail(anyString())).thenReturn(false);
        lenient().when(userRepo.existsByUsername(anyString())).thenReturn(false);

        service = new StudentRegistrationServiceImpl(
                registrationIpRateLimiter,
                emailOtpAttemptLimiter,
                passwordPolicy,
                registrationProperties,
                userProperties,
                mailProperties,
                verificationOtpMailer,
                userRepo,
                studentRepo,
                passwordEncoder,
                authenticationManager,
                jwtHelper,
                securityHelper
        );
    }

    @Test
    void register_passwordMismatch_throwsBadRequest() {
        StudentAccountRegistrationReq req = new StudentAccountRegistrationReq(
                "user1", "SecurePass123", "other", null, null, null, "a@b.c", null, "+79990001122");

        assertThatThrownBy(() -> service.registerAndIssueTokens(req, httpRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Пароли");
    }

    @Test
    void register_reservedUsername_throwsBadRequest() {
        when(registrationProperties.isReservedUsername(eq("root"), any())).thenReturn(true);
        StudentAccountRegistrationReq req = new StudentAccountRegistrationReq(
                "root", "SecurePass123", "SecurePass123", null, null, null, "a@b.c", null, "+79990001122");

        assertThatThrownBy(() -> service.registerAndIssueTokens(req, httpRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("зарезервирован");
    }

    @Test
    void register_rateLimited_throws429() {
        doThrow(new TooManyRequestsException("stop"))
                .when(registrationIpRateLimiter)
                .check(any(RegistrationIpRateLimiter.RegistrationRateBucket.class), anyString());

        assertThatThrownBy(() -> service.registerAndIssueTokens(baseReq(), httpRequest))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void register_duplicateEmail_throwsBadRequest() {
        when(studentRepo.existsByNormalizedEmail("ivan@test.local")).thenReturn(true);

        assertThatThrownBy(() -> service.registerAndIssueTokens(baseReq(), httpRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("почтой");
        verify(studentRepo, never()).save(any());
        verify(userRepo, never()).save(any());
    }

    @Test
    void register_createsDraftStudentAndUserInOneOperation() {
        stubSuccessfulAuth("newuser_x");
        when(studentRepo.save(any(StudentEnt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.registerAndIssueTokens(baseReq(), httpRequest);

        InOrder inOrder = inOrder(studentRepo, userRepo);
        inOrder.verify(studentRepo).save(studentCaptor.capture());
        inOrder.verify(userRepo).save(userCaptor.capture());

        StudentEnt draft = studentCaptor.getValue();
        assertThat(draft.getCity()).isEqualTo("Москва");
        assertThat(draft.getMiddleName()).isEqualTo("Петрович");
        assertThat(draft.isCatalogVisible()).isFalse();
        assertThat(draft.isPublicProfileConsent()).isFalse();
        assertThat(draft.getUserInformation().getFirstName()).isEqualTo("Иван");
        assertThat(draft.getUserInformation().getLastName()).isEqualTo("Иванов");
        assertThat(draft.getUserInformation().getEmail()).isEqualTo("ivan@test.local");
        assertThat(draft.getContactInformation().getPhoneNumber()).isEqualTo("+79990001122");

        UserEnt savedUser = userCaptor.getValue();
        assertThat(savedUser.getRole()).isEqualTo(RoleEnum.STUDENT);
        assertThat(savedUser.getAccountStatus()).isEqualTo(AccountStatus.PENDING_APPROVAL);
        assertThat(savedUser.getName()).isEqualTo("Иванов Иван Петрович");
        assertThat(savedUser.isPhoneVerified()).isFalse();
        assertThat(savedUser.isEmailVerified()).isFalse();
        assertThat(savedUser.getEmailOtpHash()).isEqualTo("hash");
        assertThat(savedUser.getEmailOtpExpiresAt()).isAfter(LocalDateTime.now().minusMinutes(1));
        assertThat(savedUser.getStudent()).isSameAs(draft);

        verify(verificationOtpMailer).trySendOtp(eq("ivan@test.local"), anyString(), eq(15));
    }

    @Test
    void register_mailNotSent_stillCreatesAccount() {
        stubSuccessfulAuth("newuser_x");
        when(studentRepo.save(any(StudentEnt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(verificationOtpMailer.trySendOtp(anyString(), anyString(), anyInt())).thenReturn(false);

        service.registerAndIssueTokens(baseReq(), httpRequest);

        verify(studentRepo).save(any(StudentEnt.class));
        verify(userRepo).save(any(UserEnt.class));
    }

    @Test
    void confirmEmail_wrongCode_throwsBadRequest() {
        UserEnt user = pendingStudentWithOtp();
        stubCurrentUser(user);
        when(passwordEncoder.matches("000000", "otp-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.confirmEmail("000000"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Неверный код");
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void confirmEmail_expired_throwsBadRequest() {
        UserEnt user = pendingStudentWithOtp();
        user.setEmailOtpExpiresAt(LocalDateTime.now().minusMinutes(1));
        stubCurrentUser(user);

        assertThatThrownBy(() -> service.confirmEmail("123456"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("истёк");
    }

    @Test
    void confirmEmail_lockout_throws429() {
        UserEnt user = pendingStudentWithOtp();
        stubCurrentUser(user);
        doThrow(new TooManyRequestsException("too many"))
                .when(emailOtpAttemptLimiter).checkConfirm(user.getId());

        assertThatThrownBy(() -> service.confirmEmail("123456"))
                .isInstanceOf(TooManyRequestsException.class);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void confirmEmail_success_marksVerified() {
        UserEnt user = pendingStudentWithOtp();
        stubCurrentUser(user);
        when(passwordEncoder.matches("123456", "otp-hash")).thenReturn(true);

        service.confirmEmail("123456");

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getEmailOtpHash()).isNull();
        assertThat(user.getEmailOtpExpiresAt()).isNull();
        verify(userRepo).save(user);
    }

    @Test
    void confirmEmail_alreadyVerified_isNoop() {
        UserEnt user = pendingStudentWithOtp();
        user.setEmailVerified(true);
        stubCurrentUser(user);

        service.confirmEmail("123456");

        verify(emailOtpAttemptLimiter, never()).checkConfirm(any());
        verify(userRepo, never()).save(any());
    }

    @Test
    void confirmEmail_nonStudent_forbidden() {
        UserEnt user = pendingStudentWithOtp();
        user.setRole(RoleEnum.RECRUITER);
        stubCurrentUser(user);

        assertThatThrownBy(() -> service.confirmEmail("123456"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void resendEmailConfirmation_issuesNewOtp() {
        UserEnt user = pendingStudentWithOtp();
        stubCurrentUser(user);

        service.resendEmailConfirmation();

        verify(emailOtpAttemptLimiter).checkResend(user.getId());
        verify(verificationOtpMailer).trySendOtp(eq("ivan@test.local"), anyString(), eq(15));
        verify(userRepo).save(user);
    }

    private void stubSuccessfulAuth(String username) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                new User(username, "hash", List.of()), null);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtHelper.generateAccessToken(username)).thenReturn("access");
        when(jwtHelper.generateRefreshToken(username)).thenReturn("refresh");
    }

    private void stubCurrentUser(UserEnt user) {
        when(securityHelper.getCurrentUsername()).thenReturn(user.getUsername());
        when(userRepo.findByUsernameFetchingLinks(user.getUsername())).thenReturn(Optional.of(user));
    }

    private static UserEnt pendingStudentWithOtp() {
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "Иванов Иван", "newuser_x", "hash");
        user.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        user.setEmailVerified(false);
        user.setEmailOtpHash("otp-hash");
        user.setEmailOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        user.setRegistrationEmail("ivan@test.local");
        return user;
    }

    private static StudentAccountRegistrationReq baseReq() {
        return new StudentAccountRegistrationReq(
                "newuser_x",
                "SecurePass123",
                "SecurePass123",
                "Иван",
                "Иванов",
                "Петрович",
                "ivan@test.local",
                "Москва",
                "+79990001122"
        );
    }
}
