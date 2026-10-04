package ru.ai.sin.logic.student.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ru.ai.sin.models.enums.BusynessEnum;
import ru.ai.sin.models.enums.CourseEnum;

import java.time.LocalDate;
import java.util.List;

@Schema(
        name = "CreateStudentExtendedReq",
        description = """
                Расширенное создание (`POST /student/extended`), только **ADMIN**: студент + опционально навыки, портфолио, опыт, учебные заведения.

                **`publicProfileConsent`** и **`manualSortOrder`** — как в `AddStudentReq` при `POST /student` (опционально).""")
public record CreateStudentExtendedReq(
        @Schema(description = "Город проживания")
        @Size(min = 1, max = 255, message = "City must be less than 255 characters")
        String city,

        @Schema(description = "Ссылка на HH-профиль")
        @Size(min = 1, max = 255, message = "HH link must be less than 255 characters")
        String hhLink,

        @Schema(description = "Дата рождения")
        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        @Past(message = "Birth date must be in the past")
        LocalDate birthDate,

        @Schema(description = "Краткая информация о студенте")
        @Size(max = 2000, message = "Additional info must be less than 2000 characters")
        String bio,

        @Schema(description = "Текущий курс (1–5)")
        @NotNull
        CourseEnum course,

        @Schema(description = "Тип занятости")
        @NotNull
        BusynessEnum busyness,

        @Schema(description = "Имя")
        @NotBlank
        String firstName,

        @Schema(description = "Фамилия")
        @NotBlank
        String lastName,

        @Schema(description = "Отчество")
        @Size(max = 255)
        String middleName,

        @Schema(description = "Пол; null — не указан")
        ru.ai.sin.models.enums.GenderEnum gender,

        @Schema(description = "Email")
        @Email(message = "Email should be valid")
        String email,

        @Schema(description = "Номер телефона")
        @Pattern(regexp = "\\+?\\d{1,32}", message = "Phone number must contain 1-32 digits and optional + at start")
        String phoneNumber,

        @Schema(description = "Telegram username")
        @Size(min = 1, max = 255, message = "Telegram Username must be less than 255 characters")
        String telegramUsername,

        @Schema(description = "ID специальности")
        @Positive
        long specialityId,

        @Schema(description = "Список ID существующих навыков")
        List<@Positive Long> skillsIds,
        @Schema(description = "Список навыков для привязки/создания")
        List<@Valid CreateStudentSkillReq> skills,
        @Schema(description = "Портфолио студента для создания")
        List<@Valid CreateStudentPortfolioReq> portfolio,
        @Schema(description = "Опыт работы студента для создания")
        List<@Valid CreateStudentExperienceReq> experiences,
        @Schema(description = "Образовательные записи студента для создания")
        List<@Valid CreateStudentInstitutionReq> institutions,

        @Schema(description = "Логин учётной записи (если передан — создаётся User вместе со студентом)")
        @Size(min = 3, max = 64)
        @Pattern(regexp = "^[a-zA-Z0-9_]{3,64}$", message = "Username must be 3-64 characters, letters, digits or _")
        String username,

        @Schema(description = "Пароль учётной записи (обязателен вместе с username)")
        String password,

        @Schema(description = "Согласие на публичную витрину; `null`/`false` — false в БД.")
        Boolean publicProfileConsent,

        @Schema(description = "Ручной номер сортировки; `null` — не задавать.")
        @Min(0)
        @Max(2_147_483_646)
        Integer manualSortOrder
) {
}
