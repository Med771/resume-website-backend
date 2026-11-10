package ru.ai.sin.logic.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsEventWriter {

    private final AnalyticsEventStore analyticsEventStore;
    private final ObjectMapper objectMapper;

    public void record(String eventType, String path, UUID userId, Map<String, ?> payload) {
        try {
            AnalyticsEventEnt event = new AnalyticsEventEnt();
            event.setEventType(eventType);
            event.setPath(path);
            event.setUserId(userId);
            if (payload != null) {
                event.setPayload(objectMapper.writeValueAsString(payload));
            }
            analyticsEventStore.save(event);
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("Analytics event was not saved: type={} path={}", eventType, path, ex);
        }
    }
}
