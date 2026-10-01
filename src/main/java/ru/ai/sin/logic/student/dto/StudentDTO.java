package ru.ai.sin.logic.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import ru.ai.sin.logic.skill.dto.SkillDTO;
import ru.ai.sin.models.enums.BusynessEnum;
import ru.ai.sin.models.enums.CourseEnum;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(
        name = "StudentDTO",
        description = """
                Полная карточка студента для каталога и админки после входа (`GET /student/{id}`, фильтры, ответы PUT/PATCH).
                Для анонимной витрины используется `StudentCardDTO` и отдельные эндпоинты `/public/students/...`.""")
public record StudentDTO(
        @Schema(description = "ID студента")
        @NotNull
        UUID id,

        @Schema(description = "Город проживания")
        @Size(min = 1, max = 255, message = "City must be less than 255 characters")
        String city,

        @Schema(description = "Ссылка на профиль HeadHunter")
        @Size(min = 1, max = 255, message = "HH link must be less than 255 characters")
        String hhLink,

        @Schema(description = "Дата рождения")
        @NotNull
        LocalDate birthDate,

        @Schema(description = "Краткая информация")
        @Size(max = 2000, message = "Additional info must be less than 2000 characters")
        String bio,

        @Schema(description = "Путь к изображению профиля")
        String imagePath,

        @Schema(description = "Текущий курс (1–5)")
        @NotNull
        CourseEnum course,

        @Schema(description = "Тип занятости")
        @NotNull
        BusynessEnum busyness,

        @Schema(description = "Имя")
        String firstName,
        @Schema(description = "Фамилия")
        String lastName,
        @Schema(description = "Отчество")
        String middleName,
        @Schema(description = "Пол; null — не указан")
        ru.ai.sin.models.enums.GenderEnum gender,

        @Schema(description = "Email (только для владельца и админки)")
        String email,

        @Schema(description = "Телефон")
        String phoneNumber,

        @Schema(description = "Telegram username")
        String telegramUsername,

        @Schema(description = "ID специальности")
        Long specialityId,

        @Schema(description = "Название специальности")
        @NotNull
        String speciality,

        @Schema(description = "Навыки студента")
        @NotNull
        List<SkillDTO> skills,

        @Schema(
                description = """
                        Согласие владельца карточки (выставляется **админом**) на показ **укороченной** анкеты без JWT.
                        Для публичной витрины также требуется `catalogVisible=true`.""")
        boolean publicProfileConsent,

        @Schema(description = "Видимость карточки в каталоге для рекрутёров и одобренных пользователей")
        boolean catalogVisible,

        @Schema(description = "Денормализованная метрика объёма текстовых полей; обновляется при сохранении карточки; участвует в сортировке")
        int profileTextScore,

        @Schema(description = """
                Ручной приоритет в каталоге: меньшее число — выше при сортировке по релевантности и при `sortBy=MANUAL_SORT_ORDER` ASC.
                Задаётся админом (`PUT`/`PATCH /student/{id}`). `null` — не задан (в конце списка при ASC на PostgreSQL).""")
        Integer manualSortOrder) {
}
