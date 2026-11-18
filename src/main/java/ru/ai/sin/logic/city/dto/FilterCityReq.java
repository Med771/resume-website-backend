package ru.ai.sin.logic.city.dto;

import jakarta.validation.constraints.Size;

public record FilterCityReq(
        @Size(max = 255, message = "Name must be less than 255 characters")
        String name
) {
}
