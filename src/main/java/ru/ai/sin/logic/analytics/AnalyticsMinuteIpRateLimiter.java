package ru.ai.sin.logic.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.ai.sin.config.property.AnalyticsProperties;
import ru.ai.sin.exception.models.RateLimitExceededException;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class AnalyticsMinuteIpRateLimiter {

    private final AnalyticsProperties analyticsProperties;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public void check(String clientIp) {
        int limit = analyticsProperties.getRateLimitPerIpPerMinute();
        if (limit <= 0) {
            return;
        }
        long minute = Instant.now().getEpochSecond() / 60;
        windows.compute(clientIp, (ip, w) -> {
            if (w == null || w.minute != minute) {
                return new Window(minute, 1);
            }
            if (w.count >= limit) {
                throw new RateLimitExceededException("Слишком много событий аналитики с этого IP, попробуйте позже");
            }
            w.count++;
            return w;
        });
    }

    private static final class Window {
        private final long minute;
        private int count;

        private Window(long minute, int count) {
            this.minute = minute;
            this.count = count;
        }
    }
}
