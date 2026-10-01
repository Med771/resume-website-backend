package ru.ai.sin.logic.registration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.ai.sin.config.property.RegistrationProperties;
import ru.ai.sin.exception.models.TooManyRequestsException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Лимит по IP для публичных регистраций (студенты и работодатели) — отдельные счётчики.
 */
@Component
@RequiredArgsConstructor
public class RegistrationIpRateLimiter {

    private static final long WINDOW_MS = 3_600_000L;

    private final RegistrationProperties registrationProperties;

    private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public void check(RegistrationRateBucket bucket, String clientIp) {
        int limit = switch (bucket) {
            case STUDENT -> registrationProperties.getRateLimitPerIpPerHour();
            case RECRUITER -> registrationProperties.getRateLimitRecruiterPerIpPerHour();
        };
        if (limit <= 0) {
            return;
        }
        String key = bucket.name() + "|" + clientIp;
        long now = System.currentTimeMillis();
        Deque<Long> deque = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && now - deque.peekFirst() > WINDOW_MS) {
                deque.pollFirst();
            }
            if (deque.size() >= limit) {
                String msg = bucket == RegistrationRateBucket.STUDENT
                        ? "Слишком много попыток регистрации. Попробуйте позже."
                        : "Слишком много заявок на регистрацию работодателя. Попробуйте позже.";
                throw new TooManyRequestsException(msg);
            }
            deque.addLast(now);
        }
    }

    public enum RegistrationRateBucket {
        STUDENT,
        RECRUITER
    }
}
