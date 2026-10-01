package ru.ai.sin.logic.skill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSkillReq(
        @NotBlank
        @Size(min = 1, max = 255, message = "Add skill name must be less than 255 characters")
        String name) {
}