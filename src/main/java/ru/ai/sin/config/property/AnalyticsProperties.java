package ru.ai.sin.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.analytics")
public class AnalyticsProperties {

    /**
     * Максимум событий аналитики с одного IP за минуту (скользящее окно по минутной метке).
     */
    private int rateLimitPerIpPerMinute = 300;
}
