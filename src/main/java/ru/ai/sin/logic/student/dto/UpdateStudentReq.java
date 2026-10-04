package ru.ai.sin.logic.student.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import ru.ai.sin.models.enums.BusynessEnum;
import ru.ai.sin.models.enums.CourseEnum;

import java.time.LocalDate;
import java.util.List;

@Schema(
        name = "UpdateStudentReq",
        description = """
                Полная замена полей карточки (`PUT /student/{id}`), только **ADMIN**.
                Все обязательные поля должны быть переданы; отличие от PATCH — нет семантики «пропущенное поле».

                Для `publicProfileConsent` допускается `null`: в этом случае значение в БД **не** перезаписывается.
                Для ручного номера сортировки: `manualSortOrder`, `clearManualSortOrder` — см. описание полей.""")
public record UpdateStudentReq(
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

        @Schema(description = "Текущий курс обучения (1–5)")
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

        @Schema(description = "Список ID навыков")
        @NotNull
        List<@Positive Long> skillsIds,

        @Schema(
                description = """
                        Разрешение показывать укороченную карточку на `/public/students/...` без авторизации.
                        `null` — оставить в БД прежнее значение; `true` / `false` — явно установить.""")
        Boolean publicProfileConsent,

        @Schema(description = """
                Если `true` — сбросить ручной номер сортировки в БД (`NULL`); поле `manualSortOrder` в этом случае игнорируется.""")
        Boolean clearManualSortOrder,

        @Schema(description = """
                Ручной номер для сортировки в каталоге (меньше — выше при ASC вместе с релевантностью).
                `null` — не менять текущее значение в БД (если `clearManualSortOrder` не `true`).""")
        @Min(0)
        @Max(2_147_483_646)
        Integer manualSortOrder) {
}
