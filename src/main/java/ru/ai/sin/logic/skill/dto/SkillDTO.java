package ru.ai.sin.logic.skill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SkillDTO(
        long id,

        @NotBlank
        @Size(min = 1, max = 255, message = "Name must be less than 255 characters")
        String name) {
}
