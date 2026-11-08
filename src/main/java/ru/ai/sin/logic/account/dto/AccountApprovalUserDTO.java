package ru.ai.sin.logic.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.BusynessEnum;
import ru.ai.sin.models.enums.CourseEnum;
import ru.ai.sin.models.enums.RoleEnum;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(name = "AccountApprovalUserDTO")
public record AccountApprovalUserDTO(
        UUID id,
        String username,
        String name,
        RoleEnum role,
        AccountStatus accountStatus,
        UUID studentId,
        UUID recruiterId,
        LocalDateTime createdAt,
        @Schema(description = "Устарело. Флаг подтверждения телефона через Telegram. Новая регистрация пишет false")
        boolean phoneVerified,
        @Schema(description = "Почта подтверждена кодом после саморегистрации")
        boolean emailVerified,
        String email,
        String phoneNumber,
        String telegramUsername,
        String companyName,
        String city,
        String firstName,
        String lastName,
        String speciality,
        CourseEnum course,
        BusynessEnum busyness,
        String bio,
        Integer profileTextScore,
        List<String> skills,
        @Schema(description = "Есть хотя бы одна запись портфолио")
        boolean hasPortfolio,
        @Schema(description = "Есть хотя бы одна запись опыта")
        boolean hasExperience,
        @Schema(description = "Есть хотя бы одна запись учёбы студента")
        boolean hasEducation
) {
}
