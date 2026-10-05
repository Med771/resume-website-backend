package ru.ai.sin.logic.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ai.sin.config.property.JwtProperties;
import ru.ai.sin.config.property.MailProperties;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.AuthRoleGuard;
import ru.ai.sin.helper.JwtHelper;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.auth.dto.AuthMeDTO;
import ru.ai.sin.logic.auth.dto.ChangePasswordReq;
import ru.ai.sin.logic.auth.dto.ForgotPasswordReq;
import ru.ai.sin.logic.auth.dto.LoginRequest;
import ru.ai.sin.logic.auth.dto.ResetPasswordReq;
import ru.ai.sin.logic.auth.dto.TokenPair;
import ru.ai.sin.logic.registration.ClientIpResolver;
import ru.ai.sin.logic.registration.EmailOtpAttemptLimiter;
import ru.ai.sin.logic.registration.RegistrationPasswordPolicy;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.logic.verification.VerificationOtpMailer;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.RoleEnum;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String INVALID_RESET_CODE = "Неверный или просроченный код";
    private static final List<RoleEnum> RESET_ROLES = List.of(RoleEnum.STUDENT, RoleEnum.RECRUITER);

    private final AuthenticationManager authenticationManager;
    private final JwtHelper jwtHelper;
    private final JwtProperties jwtProperties;
    private final SecurityHelper securityHelper;
    private final AuthRoleGuard authRoleGuard;
    private final UserDetailsService userDetailsService;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationPasswordPolicy passwordPolicy;
    private final EmailOtpAttemptLimiter emailOtpAttemptLimiter;
    private final MailProperties mailProperties;
    private final VerificationOtpMailer verificationOtpMailer;

    @Override
    public TokenPair login(LoginRequest request) {
        UserDetails userDetails = authenticate(request);
        authRoleGuard.requireFrontendUser(userDetails);
        return issueTokenPair(userDetails.getUsername());
    }

    @Override
    public TokenPair adminLogin(LoginRequest request) {
        UserDetails userDetails = authenticate(request);
        authRoleGuard.requireAdmin(userDetails);
        return issueTokenPair(userDetails.getUsername());
    }

    @Override
    public String refresh(HttpServletRequest request) {
        UserDetails userDetails = userDetailsFromRefreshCookie(request);
        authRoleGuard.requireFrontendUser(userDetails);
        return jwtHelper.generateAccessToken(userDetails.getUsername());
    }

    @Override
    public String adminRefresh(HttpServletRequest request) {
        UserDetails userDetails = userDetailsFromRefreshCookie(request);
        authRoleGuard.requireAdmin(userDetails);
        return jwtHelper.generateAccessToken(userDetails.getUsername());
    }

    @Override
    public AuthMeDTO getCurrentSession() {
        String username = securityHelper.getCurrentUsernameOptional()
                .orElseThrow(() -> new BadCredentialsException("Not authenticated"));
        String role = securityHelper.getCurrentRoleOptional()
                .orElseThrow(() -> new BadCredentialsException("Not authenticated"));
        UserEnt user = userRepo.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Not authenticated"));
        AccountStatus status = user.getAccountStatus() != null ? user.getAccountStatus() : AccountStatus.APPROVED;
        return new AuthMeDTO(
                user.getId(), username, role, status.getCode(), user.isEmailVerified(), user.isHintsDisabled());
    }

    @Override
    public void changePassword(ChangePasswordReq req) {
        String username = securityHelper.getCurrentUsername();
        UserEnt user = userRepo.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Неверный текущий пароль");
        }
        passwordPolicy.validate(req.newPassword());
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepo.save(user);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordReq req, HttpServletRequest request) {
        String email = normalizeEmail(req.email());
        String ip = clientIp(request);
        emailOtpAttemptLimiter.checkPasswordResetRequest(email, ip);

        List<UserEnt> users = resetCandidates(email);
        if (users.size() != 1) {
            log.info("Password reset skipped: matches={}", users.size());
            return;
        }
        UserEnt user = users.getFirst();
        String code = fourDigitCode();
        int ttl = Math.max(1, mailProperties.getOtpTtlMinutes());
        boolean sent = verificationOtpMailer.trySendPasswordReset(email, code, ttl, user.getUsername());
        if (!sent && !mailProperties.isAllowDevConfirm()) {
            log.warn("Password reset email not sent for {}", VerificationOtpMailer.maskEmail(email));
            return;
        }
        user.setPasswordResetOtpHash(passwordEncoder.encode(code));
        user.setPasswordResetOtpExpiresAt(LocalDateTime.now().plusMinutes(ttl));
        userRepo.save(user);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordReq req, HttpServletRequest request) {
        String email = normalizeEmail(req.email());
        String ip = clientIp(request);
        emailOtpAttemptLimiter.checkPasswordResetConfirm(email, ip);

        List<UserEnt> users = resetCandidates(email);
        if (users.size() != 1) {
            throw new BadRequestException(INVALID_RESET_CODE);
        }
        UserEnt user = users.getFirst();
        String submitted = req.code() == null ? "" : req.code().trim();
        if (!resetCodeMatches(user, submitted)) {
            throw new BadRequestException(INVALID_RESET_CODE);
        }
        if (!req.newPassword().equals(req.passwordConfirm())) {
            throw new BadRequestException("Пароли не совпадают");
        }
        passwordPolicy.validate(req.newPassword());
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.setPasswordResetOtpHash(null);
        user.setPasswordResetOtpExpiresAt(null);
        userRepo.save(user);
        log.info("Password reset completed: username={}", user.getUsername());
    }

    private List<UserEnt> resetCandidates(String email) {
        return userRepo.findFrontendUsersByNormalizedEmail(email, RESET_ROLES).stream()
                .filter(user -> user.getRole() == RoleEnum.STUDENT || user.getRole() == RoleEnum.RECRUITER)
                .toList();
    }

    private boolean resetCodeMatches(UserEnt user, String submitted) {
        if (user.getPasswordResetOtpHash() == null
                || user.getPasswordResetOtpExpiresAt() == null
                || user.getPasswordResetOtpExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        return passwordEncoder.matches(submitted, user.getPasswordResetOtpHash());
    }

    private static String fourDigitCode() {
        return String.format("%04d", ThreadLocalRandom.current().nextInt(10_000));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String clientIp(HttpServletRequest request) {
        String ip = ClientIpResolver.resolve(request);
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        return ip;
    }

    private UserDetails authenticate(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        if (userDetails == null) {
            throw new NotFoundException("User not found");
        }
        return userDetails;
    }

    private UserDetails userDetailsFromRefreshCookie(HttpServletRequest request) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            throw new BadCredentialsException("No refresh token");
        }
        String username = jwtHelper.getUsernameFromRefreshToken(refreshToken);
        return userDetailsService.loadUserByUsername(username);
    }

    private TokenPair issueTokenPair(String username) {
        return new TokenPair(
                jwtHelper.generateAccessToken(username),
                jwtHelper.generateRefreshToken(username));
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(c -> jwtProperties.getCookie().getRefreshTokenName().equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
