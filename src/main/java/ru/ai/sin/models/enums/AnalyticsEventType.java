package ru.ai.sin.models.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnalyticsEventType {
    PAGE_VIEW("PAGE_VIEW"),
    REGISTRATION_STARTED("REGISTRATION_STARTED"),
    REGISTRATION_COMPLETED("REGISTRATION_COMPLETED"),
    ACCOUNT_APPROVED("ACCOUNT_APPROVED"),
    APPLICATION_SUBMITTED("APPLICATION_SUBMITTED"),
    REQUEST_SUBMITTED("REQUEST_SUBMITTED"),
    CHAT_MESSAGE_SENT("CHAT_MESSAGE_SENT"),
    CHAT_TU_CONFIRMED("CHAT_TU_CONFIRMED"),
    CHAT_TU_REJECTED("CHAT_TU_REJECTED"),
    CHAT_SUCCESS("CHAT_SUCCESS");

    private final String code;

    public static AnalyticsEventType fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("eventType required");
        }
        String c = raw.trim().toUpperCase();
        for (AnalyticsEventType v : values()) {
            if (v.code.equals(c)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown eventType: " + raw);
    }
}
