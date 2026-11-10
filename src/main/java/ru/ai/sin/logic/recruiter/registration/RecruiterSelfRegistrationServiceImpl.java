package ru.ai.sin.logic.recruiter.registration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.ai.sin.config.property.MailProperties;
import ru.ai.sin.config.property.RegistrationProperties;
import ru.ai.sin.config.property.UserProperties;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.helper.JwtHelper;
import ru.ai.sin.helper.ParticipantDisplayNames;
import ru.ai.sin.logic.auth.dto.TokenPair;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.recruiter.RecruiterMapper;
import ru.ai.sin.logic.recruiter.RecruiterRepo;
import ru.ai.sin.logic.recruiter.dto.AddRecruiterReq;
import ru.ai.sin.logic.recruiter.registration.dto.RecruiterSelfRegistrationReq;
import ru.ai.sin.logic.registration.ClientIpResolver;
import ru.ai.sin.logic.registration.RegistrationIpRateLimiter;
import ru.ai.sin.logic.registration.RegistrationPasswordPolicy;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.logic.verification.VerificationOtpMailer;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.RoleEnum;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecruiterSelfRegistrationServiceImpl implements RecruiterSelfRegistrationService {

    private final RegistrationIpRateLimiter registrationIpRateLimiter;
    private final RegistrationPasswordPolicy passwordPolicy;
    private final RegistrationProperties registrationProperties;
    private final UserProperties userProperties;
    private final MailProperties mailProperties;
    private final VerificationOtpMailer verificationOtpMailer;

    private final UserRepo userRepo;
    private final RecruiterRepo recruiterRepo;
    private final RecruiterMapper recruiterMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtHelper jwtHelper;

    @Override
    @Transactional
    public TokenPair registerAndIssueTokens(RecruiterSelfRegistrationReq req, HttpServletRequest httpRequest) {
        registrationIpRateLimiter.check(
                RegistrationIpRateLimiter.RegistrationRateBucket.RECRUITER,
                ClientIpResolver.resolve(httpRequest));

        if (!req.password().equals(req.passwordConfirm())) {
            throw new BadRequestException("Пароли не совпадают");
        }
        passwordPolicy.validate(req.password());

        String username = req.username().trim();
        String email = trimToNull(req.email());
        String phone = trimToNull(req.phoneNumber());

        validateUsernameAvailable(username);

        if (email != null && recruiterRepo.existsByNormalizedEmail(email)) {
            throw conflict();
        }

        String companyName = trimToNull(req.companyName());
        String firstName = trimToNull(req.firstName());
        String lastName = trimToNull(req.lastName());
        String city = trimToNull(req.city());
        if (!StringUtils.hasText(companyName)) {
            throw new BadRequestException("Укажите название компании");
        }
        if (!StringUtils.hasText(firstName) || !StringUtils.hasText(lastName)) {
            throw new BadRequestException("Укажите имя и фамилию");
        }
        if (email == null) {
            throw new BadRequestException("Укажите email");
        }

        AddRecruiterReq addRecruiterReq = new AddRecruiterReq(
                companyName,
                firstName,
                lastName,
                email,
                phone,
                trimToNull(req.telegramUsername())
        );
        RecruiterEnt recruiter = recruiterMapper.toEntity(addRecruiterReq);
        recruiter.setCity(city);
        recruiter.setMiddleName(trimToNull(req.middleName()));
        try {
            recruiter = recruiterRepo.save(recruiter);
        } catch (DataIntegrityViolationException ex) {
            throw conflict();
        }

        UserEnt user = new UserEnt(
                RoleEnum.RECRUITER,
                buildDisplayName(req),
                username,
                passwordEncoder.encode(req.password())
        );
        user.setRecruiter(recruiter);
        user.setPhoneVerified(false);
        user.setEmailVerified(false);
        user.setRegistrationEmail(email);
        user.setAccountStatus(AccountStatus.PENDING_APPROVAL);
        issueEmailOtp(user, email);

        try {
            userRepo.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw conflict();
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, req.password()));
        log.info("Recruiter registered with PENDING approval: username={} recruiterId={}", username, recruiter.getId());
        return new TokenPair(
                jwtHelper.generateAccessToken(username),
                jwtHelper.generateRefreshToken(username)
        );
    }

    private void validateUsernameAvailable(String username) {
        if (userRepo.existsByUsername(username)) {
            throw conflict();
        }
        if (registrationProperties.isReservedUsername(username, userProperties)) {
            throw new BadRequestException("Этот логин зарезервирован");
        }
    }

    private void issueEmailOtp(UserEnt user, String email) {
        String code = String.format("%04d", ThreadLocalRandom.current().nextInt(10_000));
        int ttl = Math.max(1, mailProperties.getOtpTtlMinutes());
        user.setEmailOtpHash(passwordEncoder.encode(code));
        user.setEmailOtpExpiresAt(LocalDateTime.now().plusMinutes(ttl));
        boolean sent = verificationOtpMailer.trySendStudentEmailOtp(email, code, ttl);
        if (!sent && !mailProperties.isAllowDevConfirm()) {
            log.warn("Email OTP not sent for username={}", user.getUsername());
        }
    }

    private static String buildDisplayName(RecruiterSelfRegistrationReq req) {
        return ParticipantDisplayNames.fromFio(req.lastName(), req.firstName(), req.middleName());
    }

    private static BadRequestException conflict() {
        return new BadRequestException(
                "Не удалось завершить регистрацию. Проверьте данные или войдите, если аккаунт уже есть.");
    }

    private static String trimToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}
