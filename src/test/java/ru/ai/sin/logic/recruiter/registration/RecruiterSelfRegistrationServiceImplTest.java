package ru.ai.sin.logic.recruiter.registration;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import ru.ai.sin.helper.JwtHelper;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.recruiter.RecruiterMapper;
import ru.ai.sin.logic.recruiter.RecruiterRepo;
import ru.ai.sin.logic.recruiter.dto.AddRecruiterReq;
import ru.ai.sin.logic.recruiter.registration.dto.RecruiterSelfRegistrationReq;
import ru.ai.sin.logic.registration.RegistrationIpRateLimiter;
import ru.ai.sin.logic.registration.RegistrationPasswordPolicy;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.logic.verification.VerificationOtpMailer;
import ru.ai.sin.models.enums.AccountStatus;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecruiterSelfRegistrationServiceImplTest {

    @Mock
    private RegistrationIpRateLimiter registrationIpRateLimiter;
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
    private RecruiterRepo recruiterRepo;
    @Mock
    private RecruiterMapper recruiterMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtHelper jwtHelper;
    @Mock
    private HttpServletRequest httpRequest;

    private RecruiterSelfRegistrationServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(registrationProperties.isReservedUsername(anyString(), any())).thenReturn(false);
        lenient().when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(mailProperties.getOtpTtlMinutes()).thenReturn(15);
        lenient().when(mailProperties.isAllowDevConfirm()).thenReturn(true);
        lenient().when(verificationOtpMailer.trySendStudentEmailOtp(anyString(), anyString(), anyInt())).thenReturn(true);
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("hash");
        lenient().when(userRepo.existsByUsername(anyString())).thenReturn(false);
        lenient().when(recruiterRepo.existsByNormalizedEmail(anyString())).thenReturn(false);
        lenient().when(recruiterMapper.toEntity(any(AddRecruiterReq.class))).thenAnswer(invocation -> {
            AddRecruiterReq req = invocation.getArgument(0);
            RecruiterEnt recruiter = new RecruiterEnt();
            recruiter.getUserInformation().setFirstName(req.firstName());
            recruiter.getUserInformation().setLastName(req.lastName());
            recruiter.getUserInformation().setEmail(req.email());
            recruiter.setCompanyName(req.companyName());
            return recruiter;
        });
        lenient().when(recruiterRepo.save(any(RecruiterEnt.class))).thenAnswer(invocation -> {
            RecruiterEnt recruiter = invocation.getArgument(0);
            recruiter.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
            return recruiter;
        });
        lenient().when(userRepo.save(any(UserEnt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Authentication auth = new UsernamePasswordAuthenticationToken(new User("boss", "hash", List.of()), null);
        lenient().when(authenticationManager.authenticate(any())).thenReturn(auth);
        lenient().when(jwtHelper.generateAccessToken(anyString())).thenReturn("access");
        lenient().when(jwtHelper.generateRefreshToken(anyString())).thenReturn("refresh");

        service = new RecruiterSelfRegistrationServiceImpl(
                registrationIpRateLimiter,
                passwordPolicy,
                registrationProperties,
                userProperties,
                mailProperties,
                verificationOtpMailer,
                userRepo,
                recruiterRepo,
                recruiterMapper,
                passwordEncoder,
                authenticationManager,
                jwtHelper
        );
    }

    @Test
    void register_withoutOptionalFields_sendsCodeAndLeavesMiddleNameEmpty() {
        service.registerAndIssueTokens(request(null, null, null, "Иван", "Иванов"), httpRequest);

        ArgumentCaptor<RecruiterEnt> recruiterCaptor = ArgumentCaptor.forClass(RecruiterEnt.class);
        verify(recruiterRepo).save(recruiterCaptor.capture());
        assertThat(recruiterCaptor.getValue().getMiddleName()).isNull();
        assertThat(recruiterCaptor.getValue().getCity()).isNull();

        ArgumentCaptor<UserEnt> userCaptor = ArgumentCaptor.forClass(UserEnt.class);
        verify(userRepo).save(userCaptor.capture());
        UserEnt user = userCaptor.getValue();
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getRegistrationEmail()).isEqualTo("boss@test.local");
        assertThat(user.getName()).isEqualTo("Иванов Иван");
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.PENDING_APPROVAL);
        assertThat(user.getEmailOtpHash()).isEqualTo("hash");
        verify(verificationOtpMailer).trySendStudentEmailOtp(eq("boss@test.local"), anyString(), eq(15));
    }

    @Test
    void register_withMiddleName_storesItAndBuildsDisplayNameFromFio() {
        service.registerAndIssueTokens(request("Босс", "Москва", "Петрович", "Иван", "Иванов"), httpRequest);

        ArgumentCaptor<RecruiterEnt> recruiterCaptor = ArgumentCaptor.forClass(RecruiterEnt.class);
        verify(recruiterRepo).save(recruiterCaptor.capture());
        assertThat(recruiterCaptor.getValue().getMiddleName()).isEqualTo("Петрович");

        ArgumentCaptor<UserEnt> userCaptor = ArgumentCaptor.forClass(UserEnt.class);
        verify(userRepo).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getName()).isEqualTo("Иванов Иван Петрович");
    }

    @Test
    void register_withoutCompany_throwsBadRequest() {
        RecruiterSelfRegistrationReq base = request(null, null, null, "Иван", "Иванов");
        RecruiterSelfRegistrationReq req = new RecruiterSelfRegistrationReq(
                base.username(), base.password(), base.passwordConfirm(), base.name(),
                " ", base.city(), base.firstName(), base.lastName(), base.middleName(),
                base.email(), base.phoneNumber(), base.telegramUsername(), base.marketingConsent());

        assertThatThrownBy(() -> service.registerAndIssueTokens(req, httpRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("компани");
        verify(recruiterRepo, never()).save(any());
    }

    @Test
    void register_withoutEmail_throwsBadRequest() {
        RecruiterSelfRegistrationReq req = new RecruiterSelfRegistrationReq(
                "boss", "SecurePass123", "SecurePass123", null,
                "ООО Ромашка", null, "Иван", "Иванов", null,
                " ", null, null, null);

        assertThatThrownBy(() -> service.registerAndIssueTokens(req, httpRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("email");
    }

    private static RecruiterSelfRegistrationReq request(
            String name,
            String city,
            String middleName,
            String firstName,
            String lastName
    ) {
        return new RecruiterSelfRegistrationReq(
                "boss",
                "SecurePass123",
                "SecurePass123",
                name,
                "ООО Ромашка",
                city,
                firstName,
                lastName,
                middleName,
                "boss@test.local",
                null,
                null,
                null
        );
    }
}
