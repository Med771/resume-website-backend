package ru.ai.sin.models.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccountStatus {
    PENDING_APPROVAL("PENDING_APPROVAL"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED");

    private final String code;

    public static AccountStatus fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return APPROVED;
        }
        String c = raw.trim().toUpperCase();
        for (AccountStatus v : values()) {
            if (v.code.equals(c)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown accountStatus: " + raw);
    }
}
