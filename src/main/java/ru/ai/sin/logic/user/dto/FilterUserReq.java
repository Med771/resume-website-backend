package ru.ai.sin.logic.user.dto;

import jakarta.validation.constraints.Size;

public record FilterUserReq(
        @Size(max = 64)
        String username
) {
}
