package ru.ai.sin.logic.verification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ai.sin.config.property.MailProperties;
import ru.ai.sin.config.property.TelegramProperties;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.logic.verification.dto.PhoneVerificationStartReq;
import ru.ai.sin.logic.verification.dto.PhoneVerificationStartRes;
import ru.ai.sin.logic.verification.dto.PhoneVerificationStatusRes;
import ru.ai.sin.models.enums.PhoneVerificationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneVerificationServiceImpl implements PhoneVerificationService {

    private final PhoneVerificationRepo phoneVerificationRepo;
    private final TelegramProperties telegramProperties;
    private final MailProperties mailProperties;
    private final TelegramBotClient telegramBotClient;
    private final VerificationOtpMailer verificationOtpMailer;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public PhoneVerificationStartRes startVerification(PhoneVerificationStartReq req) {
        String email = normalizeEmail(req.email());
        ensureCanStartVerification(email);

        String normalized = normalizePhone(req.phoneNumber());
        expirePendingSessions(
                phoneVerificationRepo.findByPhoneNumberAndStatusOrderByCreatedAtDesc(
                        normalized, PhoneVerificationStatus.PENDING),
                null);

        PhoneVerificationEnt ent = new PhoneVerificationEnt();
        ent.setPhoneNumber(normalized);
        ent.setStatus(PhoneVerificationStatus.PENDING);
        ent.setCreatedAt(LocalDateTime.now());
        ent.setExpiresAt(LocalDateTime.now().plusMinutes(telegramProperties.getVerificationTtlMinutes()));

        boolean emailOtpSent = false;
        if (email != null && mailProperties.isEnabled()) {
            String otp = generateOtpCode();
            ent.setEmail(email);
            ent.setOtpCodeHash(passwordEncoder.encode(otp));
            ent = phoneVerificationRepo.save(ent);
            emailOtpSent = verificationOtpMailer.trySendOtp(
                    email, otp, telegramProperties.getVerificationTtlMinutes());
            if (!emailOtpSent) {
                ent.setOtpCodeHash(null);
                phoneVerificationRepo.save(ent);
                if (!hasVerificationFallback()) {
                    throw new BadRequestException("Не удалось отправить код на почту. Попробуйте позже.");
                }
                log.warn("Email OTP not sent for verificationId={}, fallback available", ent.getId());
            } else {
                log.info("Phone verification started with email OTP: verificationId={}", ent.getId());
            }
        } else {
            ent = phoneVerificationRepo.save(ent);
        }

        String botUsername = resolveBotUsername();
        String deepLink = "https://t.me/" + botUsername + "?start=" + ent.getId();

        return new PhoneVerificationStartRes(
                ent.getId(),
                botUsername,
                deepLink,
                telegramProperties.getVerificationTtlMinutes(),
                emailOtpSent
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PhoneVerificationStatusRes getStatus(UUID verificationId) {
        PhoneVerificationEnt ent = phoneVerificationRepo.findById(verificationId)
                .orElseThrow(() -> new NotFoundException("Сессия верификации не найдена"));
        return new PhoneVerificationStatusRes(ent.getId(), resolveEffectiveStatus(ent));
    }

    @Override
    @Transactional(readOnly = true)
    public void requireConfirmed(UUID verificationId, String phoneNumber) {
        PhoneVerificationEnt ent = phoneVerificationRepo.findById(verificationId)
                .orElseThrow(() -> new BadRequestException("Подтвердите номер телефона"));

        PhoneVerificationStatus status = resolveEffectiveStatus(ent);
        if (status != PhoneVerificationStatus.CONFIRMED) {
            throw new BadRequestException("Номер телефона ещё не подтверждён");
        }
        if (!normalizePhone(phoneNumber).equals(ent.getPhoneNumber())) {
            throw new BadRequestException("Номер телефона не совпадает с подтверждённым");
        }
    }

    @Override
    @Transactional
    public PhoneVerificationStatusRes confirmWithDevCode(UUID verificationId, String code) {
        String submitted = code != null ? code.trim() : "";

        PhoneVerificationEnt ent = phoneVerificationRepo.findById(verificationId)
                .orElseThrow(() -> new NotFoundException("Сессия верификации не найдена"));

        PhoneVerificationStatus status = resolveEffectiveStatus(ent);
        if (status == PhoneVerificationStatus.EXPIRED) {
            throw new BadRequestException("Время подтверждения истекло");
        }
        if (status == PhoneVerificationStatus.CONFIRMED) {
            return new PhoneVerificationStatusRes(ent.getId(), PhoneVerificationStatus.CONFIRMED);
        }

        if (ent.getOtpCodeHash() != null) {
            if (!passwordEncoder.matches(submitted, ent.getOtpCodeHash())) {
                throw new BadRequestException("Неверный код подтверждения");
            }
            return markConfirmed(ent, "email-otp");
        }

        if (!telegramProperties.isAllowDevConfirm()) {
            throw new BadRequestException("Подтверждение кодом недоступно");
        }
        String expected = telegramProperties.getDevConfirmCode();
        if (expected == null || expected.isBlank()) {
            throw new BadRequestException("Тестовый код не настроен");
        }
        if (!expected.equals(submitted)) {
            throw new BadRequestException("Неверный код подтверждения");
        }

        return markConfirmed(ent, "dev-code");
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public void handleWebhookUpdate(Map<String, Object> update) {
        if (!telegramProperties.isEnabled()) {
            return;
        }
        Object messageObj = update.get("message");
        if (!(messageObj instanceof Map<?, ?> message)) {
            return;
        }

        Object chatObj = message.get("chat");
        if (!(chatObj instanceof Map<?, ?> chat)) {
            return;
        }
        Object chatIdObj = chat.get("id");
        if (chatIdObj == null) {
            return;
        }
        long chatId = Long.parseLong(chatIdObj.toString());

        Object contactObj = message.get("contact");
        if (contactObj instanceof Map<?, ?> contact) {
            handleContact(chatId, contact);
            return;
        }

        Object textObj = message.get("text");
        if (textObj instanceof String text && text.startsWith("/start")) {
            handleStart(chatId, text);
        }
    }

    private PhoneVerificationStatusRes markConfirmed(PhoneVerificationEnt ent, String channel) {
        ent.setStatus(PhoneVerificationStatus.CONFIRMED);
        ent.setConfirmedAt(LocalDateTime.now());
        ent.setOtpCodeHash(null);
        phoneVerificationRepo.save(ent);
        log.info("Phone verified via {}: verificationId={} phone={}", channel, ent.getId(), ent.getPhoneNumber());
        return new PhoneVerificationStatusRes(ent.getId(), PhoneVerificationStatus.CONFIRMED);
    }

    private void handleStart(long chatId, String text) {
        String[] parts = text.trim().split("\\s+", 2);
        if (parts.length < 2) {
            telegramBotClient.sendMessage(chatId,
                    "Для подтверждения номера откройте ссылку с сайта регистрации.");
            return;
        }
        UUID verificationId;
        try {
            verificationId = UUID.fromString(parts[1].trim());
        } catch (IllegalArgumentException ex) {
            telegramBotClient.sendMessage(chatId, "Неверная ссылка верификации.");
            return;
        }

        PhoneVerificationEnt ent = phoneVerificationRepo.findById(verificationId).orElse(null);
        if (ent == null || resolveEffectiveStatus(ent) != PhoneVerificationStatus.PENDING) {
            telegramBotClient.sendMessage(chatId, "Сессия верификации не найдена или уже завершена.");
            return;
        }

        expirePendingSessions(
                phoneVerificationRepo.findByTelegramChatIdAndStatusOrderByCreatedAtDesc(
                        chatId, PhoneVerificationStatus.PENDING),
                ent.getId());
        ent.setTelegramChatId(chatId);
        phoneVerificationRepo.save(ent);

        telegramBotClient.sendContactRequest(chatId,
                "Нажмите кнопку ниже и поделитесь номером телефона для подтверждения.");
    }

    private void handleContact(long chatId, Map<?, ?> contact) {
        Object phoneObj = contact.get("phone_number");
        Object userIdObj = contact.get("user_id");
        if (phoneObj == null) {
            return;
        }
        String phone = normalizePhone(phoneObj.toString());
        String telegramUserId = userIdObj != null ? userIdObj.toString() : null;

        PhoneVerificationEnt pending = findActivePendingForTelegramChat(chatId, phone).orElse(null);

        if (pending != null && !phone.equals(pending.getPhoneNumber())) {
            telegramBotClient.sendMessage(chatId,
                    "Номер телефона не совпадает с указанным на сайте. Начните верификацию заново.");
            return;
        }

        if (pending == null) {
            telegramBotClient.sendMessage(chatId,
                    "Не найдена активная сессия верификации для этого номера. Начните с сайта.");
            return;
        }

        pending.setStatus(PhoneVerificationStatus.CONFIRMED);
        pending.setConfirmedAt(LocalDateTime.now());
        pending.setTelegramUserId(telegramUserId);
        pending.setOtpCodeHash(null);
        phoneVerificationRepo.save(pending);

        telegramBotClient.removeKeyboard(chatId, "Номер телефона подтверждён. Вернитесь на сайт и завершите регистрацию.");
        log.info("Phone verified: verificationId={} phone={}", pending.getId(), phone);
    }

    private Optional<PhoneVerificationEnt> findActivePendingForTelegramChat(long chatId, String phone) {
        return phoneVerificationRepo
                .findByTelegramChatIdAndStatusOrderByCreatedAtDesc(chatId, PhoneVerificationStatus.PENDING)
                .stream()
                .filter(v -> resolveEffectiveStatus(v) == PhoneVerificationStatus.PENDING)
                .filter(v -> phone == null || phone.equals(v.getPhoneNumber()))
                .findFirst();
    }

    private void expirePendingSessions(List<PhoneVerificationEnt> sessions, UUID keepId) {
        LocalDateTime now = LocalDateTime.now();
        for (PhoneVerificationEnt session : sessions) {
            if (keepId != null && keepId.equals(session.getId())) {
                continue;
            }
            if (session.getStatus() != PhoneVerificationStatus.PENDING) {
                continue;
            }
            session.setStatus(PhoneVerificationStatus.EXPIRED);
            session.setExpiresAt(now);
            phoneVerificationRepo.save(session);
        }
    }

    private PhoneVerificationStatus resolveEffectiveStatus(PhoneVerificationEnt ent) {
        if (ent.getStatus() == PhoneVerificationStatus.PENDING && ent.getExpiresAt().isBefore(LocalDateTime.now())) {
            return PhoneVerificationStatus.EXPIRED;
        }
        return ent.getStatus();
    }

    private boolean hasVerificationFallback() {
        return telegramProperties.isEnabled() || telegramProperties.isAllowDevConfirm();
    }

    private void ensureCanStartVerification(String email) {
        if (telegramProperties.isAllowDevConfirm()) {
            return;
        }
        if (telegramProperties.isEnabled()) {
            return;
        }
        if (mailProperties.isEnabled() && email != null) {
            return;
        }
        if (mailProperties.isEnabled()) {
            throw new BadRequestException("Укажите email для отправки кода подтверждения");
        }
        ensureTelegramEnabled();
    }

    private void ensureTelegramEnabled() {
        if (!telegramProperties.isEnabled()) {
            throw new BadRequestException("Верификация временно недоступна");
        }
        if (telegramProperties.getBotUsername() == null || telegramProperties.getBotUsername().isBlank()) {
            throw new BadRequestException("Telegram-бот не настроен");
        }
        if (telegramProperties.getBotToken() == null || telegramProperties.getBotToken().isBlank()) {
            throw new BadRequestException("Telegram-бот не настроен");
        }
    }

    private String resolveBotUsername() {
        String username = null;
        if (telegramProperties.getBotUsername() != null && !telegramProperties.getBotUsername().isBlank()) {
            username = telegramProperties.getBotUsername().trim();
        } else if (telegramProperties.isAllowDevConfirm()) {
            return "dev_bot";
        } else if (!telegramProperties.isEnabled()) {
            return "mail_only";
        } else {
            throw new BadRequestException("Telegram-бот не настроен");
        }
        if (username.startsWith("@")) {
            username = username.substring(1);
        }
        return username;
    }

    static String generateOtpCode() {
        return String.format("%04d", ThreadLocalRandom.current().nextInt(10_000));
    }

    static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    static String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("8") && digits.length() == 11) {
            return "7" + digits.substring(1);
        }
        return digits;
    }
}
