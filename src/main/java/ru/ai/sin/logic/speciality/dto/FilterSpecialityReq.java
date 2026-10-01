package ru.ai.sin.logic.speciality.dto;

import jakarta.validation.constraints.Size;


public record FilterSpecialityReq(
        @Size(min = 1, max = 255, message = "Name must be less than 255 characters")
        String name) {
}
