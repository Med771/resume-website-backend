package ru.ai.sin.logic.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddCompanyReq(
        @NotBlank
        @Size(min = 1, max = 255, message = "Add company name must be less than 255 characters")
        String name) {
}