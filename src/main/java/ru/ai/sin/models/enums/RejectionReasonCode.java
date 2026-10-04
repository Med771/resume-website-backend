package ru.ai.sin.models.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RejectionReasonCode {
    NOT_A_FIT("NOT_A_FIT"),
    NO_RESPONSE("NO_RESPONSE"),
    CANDIDATE_DECLINED("CANDIDATE_DECLINED"),
    EMPLOYER_DECLINED("EMPLOYER_DECLINED"),
    OTHER("OTHER");

    private final String code;

    public static RejectionReasonCode fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("rejectionReasonCode required");
        }
        String c = raw.trim().toUpperCase();
        for (RejectionReasonCode v : values()) {
            if (v.code.equals(c)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown rejectionReasonCode: " + raw);
    }
}
