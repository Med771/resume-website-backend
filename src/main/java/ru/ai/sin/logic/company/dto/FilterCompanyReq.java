package ru.ai.sin.logic.company.dto;

import jakarta.validation.constraints.Size;

public record FilterCompanyReq(
        @Size(min = 1, max = 255, message = "Name must be less than 255 characters")
        String name
) {
}
