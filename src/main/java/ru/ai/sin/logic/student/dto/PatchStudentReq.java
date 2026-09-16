package ru.ai.sin.logic.student.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ru.ai.sin.models.enums.BusynessEnum;
import ru.ai.sin.models.enums.CourseEnum;

import java.time.LocalDate;
import java.util.List;

@Schema(
        name = "PatchStudentReq",
        description = """
                Частичное обновление карточки студента (`PATCH /student/{id}`), только **ADMIN**.
                Для каждого поля: значение `null` в JSON означает «поле не изменять».

                Исключение по смыслу то же для `skillsIds`: `null` — не трогать список навыков.
                Для ручного номера: `clearManualSortOrder=true` сбрасывает значение в БД; иначе непустой `manualSortOrder` записывает число.""")
public record PatchStudentReq(
        @Schema(description = "Город проживания")
        @Size(min = 1, max = 255, message = "City must be less than 255 characters")
        String city,

        @Schema(description = "Ссылка на HH-профиль")
        @Size(min = 1, max = 255, message = "HH link must be less than 255 characters")
        String hhLink,

        @Schema(description = "Дата рождения")
        @JsonFormat(pattern = "yyyy-MM-dd")
        @Past(message = "Birth date must be in the past")
        LocalDate birthDate,

        @Schema(description = "Краткая информация о студенте")
        @Size(max = 2000, message = "Additional info must be less than 2000 characters")
        String bio,

        @Schema(description = "Текущий курс (1–5)")
        CourseEnum course,
        @Schema(description = "Тип занятости")
        BusynessEnum busyness,

        @Schema(description = "Имя")
        @Size(min = 1, max = 255, message = "First name must be less than 255 characters")
        String firstName,

        @Schema(description = "Фамилия")
        @Size(min = 1, max = 255, message = "Last name must be less than 255 characters")
        String lastName,

        @Schema(description = "Отчество")
        @Size(max = 255)
        String middleName,

        @Schema(description = "Пол; null — не менять")
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
        Long specialityId,

        @Schema(description = "Список ID навыков")
        List<@Positive Long> skillsIds,

        @Schema(
                description = """
                        Разрешение показывать **укороченную** карточку на публичной витрине (`/public/students/...`) без JWT.
                        Не влияет на выдачу для рекрутеров по `POST /student/...` — там действуют отдельные правила (в т.ч. `catalogVisible`).

                        `null` — не менять текущее значение в БД.""")
        Boolean publicProfileConsent,

        @Schema(description = "Видимость в каталоге рекрутёров; `null` — не менять")
        Boolean catalogVisible,

        @Schema(description = """
                Если `true` — сбросить ручной номер сортировки (`NULL` в БД); иначе при непустом `manualSortOrder` — записать число.""")
        Boolean clearManualSortOrder,

        @Schema(description = "Ручной номер для порядка в каталоге; `null` — не менять (если не задан `clearManualSortOrder`).")
        @Min(0)
        @Max(2_147_483_646)
        Integer manualSortOrder
) {
}
