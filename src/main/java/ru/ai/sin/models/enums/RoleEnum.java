package ru.ai.sin.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum RoleEnum {
    STUDENT("STUDENT"),
    RECRUITER("RECRUITER"),
    ADMIN("ADMIN");

    private final String role;

    public static RoleEnum fromRole(String role) {
        for (RoleEnum status: values()) {
            if (status.getRole().equals(role)) return status;
        }
        throw new IllegalArgumentException("Unknown role: " + role);
    }
}
