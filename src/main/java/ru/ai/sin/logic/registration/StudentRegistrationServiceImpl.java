package ru.ai.sin.logic.registration;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ai.sin.config.property.MailProperties;
import ru.ai.sin.config.property.RegistrationProperties;
import ru.ai.sin.config.property.UserProperties;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.ForbiddenException;
import ru.ai.sin.helper.JwtHelper;
import ru.ai.sin.helper.ParticipantDisplayNames;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.auth.dto.TokenPair;
import ru.ai.sin.logic.registration.dto.StudentAccountRegistrationReq;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.student.StudentRepo;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.logic.verification.VerificationOtpMailer;
import ru.ai.sin.models.embeddables.ContactInformation;
import ru.ai.sin.models.embeddables.UserInformation;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.RoleEnum;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentRegistrationServiceImpl implements StudentRegistrationService {

    private final RegistrationIpRateLimiter registrationIpRateLimiter;
    private final EmailOtpAttemptLimiter emailOtpAttemptLimiter;
    private final RegistrationPasswordPolicy passwordPolicy;
    private final RegistrationProperties registrationProperties;
    private final UserProperties userProperties;
    private final MailProperties mailProperties;
    private final VerificationOtpMailer verificationOtpMailer;

    private final UserRepo userRepo;
    private final StudentRepo studentRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtHelper jwtHelper;
    private final SecurityHelper securityHelper;

    @Override
    @Transactional
    public TokenPair registerAndIssueTokens(StudentAccountRegistrationReq req, HttpServletRequest httpRequest) {
        registrationIpRateLimiter.check(
                RegistrationIpRateLimiter.RegistrationRateBucket.STUDENT,
                ClientIpResolver.resolve(httpRequest));

        if (!req.password().equals(req.passwordConfirm())) {
            throw new BadRequestException("Пароли не совпадают");
        }
        passwordPolicy.validate(req.password());

        String username = req.username().trim();
        String phone = req.phoneNumber().trim();
        String email = req.email().trim();

        if (userRepo.existsByUsername(username)) {
            log.warn("Student registration: username already exists");
            throw conflict();
        }
        if (registrationProperties.isReservedUsername(username, userProperties)) {
            throw new BadRequestException("Этот логин зарезервирован");
        }
        if (studentRepo.existsByNormalizedEmail(email)) {
            throw new BadRequestException("Пользователь с такой почтой уже зарегистрирован");
        }

        StudentEnt student = studentRepo.save(createDraftStudent(req, phone, email));

        UserEnt user = new UserEnt(
                RoleEnum.STUDENT,
                ParticipantDisplayNames.fromFio(req.lastName(), req.firstName(), req.middleName()),
                username,
                passwordEncoder.encode(req.password())
        );
        user.setPhoneVerified(false);
        user.setEmailVerified(false);
        user.setAccountStatus(AccountStatus.PENDING_APPROVAL);
        user.setRegistrationPhone(phone);
        user.setRegistrationEmail(email);
        user.setStudent(student);
        issueEmailOtp(user, email);

        try {
            userRepo.save(user);
        } catch (DataIntegrityViolationException ex) {
            log.warn("User registration data conflict: {}", ex.getMessage());
            throw conflict();
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, req.password()));
        log.info("Student account registered: username={} studentId={}", username, student.getId());
        return new TokenPair(
                jwtHelper.generateAccessToken(username),
                jwtHelper.generateRefreshToken(username));
    }

    @Override
    @Transactional
    public void confirmEmail(String code) {
        UserEnt user = requireCurrentStudentUser();
        if (user.isEmailVerified()) {
            return;
        }
        emailOtpAttemptLimiter.checkConfirm(user.getId());
        String submitted = code != null ? code.trim() : "";
        if (user.getEmailOtpHash() == null
                || user.getEmailOtpExpiresAt() == null
                || user.getEmailOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Код подтверждения истёк. Запросите новый.");
        }
        if (!passwordEncoder.matches(submitted, user.getEmailOtpHash())) {
            throw new BadRequestException("Неверный код подтверждения");
        }
        user.setEmailVerified(true);
        user.setEmailOtpHash(null);
        user.setEmailOtpExpiresAt(null);
        userRepo.save(user);
        log.info("Student email verified: username={}", user.getUsername());
    }

    @Override
    @Transactional
    public void resendEmailConfirmation() {
        UserEnt user = requireCurrentStudentUser();
        if (user.isEmailVerified()) {
            throw new BadRequestException("Почта уже подтверждена");
        }
        emailOtpAttemptLimiter.checkResend(user.getId());
        String email = resolveEmail(user);
        if (email == null) {
            throw new BadRequestException("У аккаунта не указана почта");
        }
        issueEmailOtp(user, email);
        userRepo.save(user);
    }

    private void issueEmailOtp(UserEnt user, String email) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        int ttl = Math.max(1, mailProperties.getOtpTtlMinutes());
        user.setEmailOtpHash(passwordEncoder.encode(code));
        user.setEmailOtpExpiresAt(LocalDateTime.now().plusMinutes(ttl));
        boolean sent = verificationOtpMailer.trySendOtp(email, code, ttl);
        if (!sent && !mailProperties.isAllowDevConfirm()) {
            log.warn("Email OTP not sent for username={}", user.getUsername());
        }
    }

    private UserEnt requireCurrentStudentUser() {
        String username = securityHelper.getCurrentUsername();
        UserEnt user = userRepo.findByUsernameFetchingLinks(username)
                .orElseThrow(() -> new ForbiddenException("Пользователь не найден"));
        if (user.getRole() != RoleEnum.STUDENT) {
            throw new ForbiddenException("Подтверждение почты доступно только студенту");
        }
        return user;
    }

    private static String resolveEmail(UserEnt user) {
        if (user.getRegistrationEmail() != null && !user.getRegistrationEmail().isBlank()) {
            return user.getRegistrationEmail().trim();
        }
        if (user.getStudent() != null
                && user.getStudent().getUserInformation() != null
                && user.getStudent().getUserInformation().getEmail() != null
                && !user.getStudent().getUserInformation().getEmail().isBlank()) {
            return user.getStudent().getUserInformation().getEmail().trim();
        }
        return null;
    }

    private static StudentEnt createDraftStudent(StudentAccountRegistrationReq req, String phone, String email) {
        StudentEnt student = new StudentEnt();
        student.setCity(emptyToNull(req.city()));
        student.setMiddleName(emptyToNull(req.middleName()));
        student.setCatalogVisible(false);
        student.setPublicProfileConsent(false);

        UserInformation userInfo = new UserInformation();
        userInfo.setFirstName(emptyToNull(req.firstName()));
        userInfo.setLastName(emptyToNull(req.lastName()));
        userInfo.setEmail(email);
        student.setUserInformation(userInfo);

        ContactInformation contact = new ContactInformation();
        contact.setPhoneNumber(phone);
        student.setContactInformation(contact);

        return student;
    }

    private static BadRequestException conflict() {
        return new BadRequestException(
                "Не удалось завершить регистрацию. Проверьте данные или войдите, если аккаунт уже есть.");
    }

    private static String emptyToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}
