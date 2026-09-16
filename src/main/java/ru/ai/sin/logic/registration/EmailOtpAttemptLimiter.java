package ru.ai.sin.logic.registration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.ai.sin.config.property.RegistrationProperties;
import ru.ai.sin.exception.models.TooManyRequestsException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class EmailOtpAttemptLimiter {

    private static final long WINDOW_MS = 3_600_000L;

    private final RegistrationProperties registrationProperties;
    private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public void checkConfirm(UUID userId) {
        check("confirm|" + userId, registrationProperties.getEmailConfirmMaxAttemptsPerHour(),
                "Слишком много попыток подтверждения почты. Попробуйте позже.");
    }

    public void checkResend(UUID userId) {
        check("resend|" + userId, registrationProperties.getEmailResendMaxPerHour(),
                "Слишком много повторных отправок кода. Попробуйте позже.");
    }

    private void check(String key, int limit, String message) {
        if (limit <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        Deque<Long> deque = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && now - deque.peekFirst() > WINDOW_MS) {
                deque.pollFirst();
            }
            if (deque.size() >= limit) {
                throw new TooManyRequestsException(message);
            }
            deque.addLast(now);
        }
    }
}
