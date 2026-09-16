package ru.ai.sin.logic.student.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ru.ai.sin.models.enums.BusynessEnum;
import ru.ai.sin.models.enums.CourseEnum;

import java.time.LocalDate;
import java.util.List;

@Schema(
        name = "PatchStudentMeReq",
        description = """
                Частичное обновление своей карточки (`PATCH /student/me`), только **STUDENT**.
                `null` у поля — не менять. `skillsIds: null` — не трогать навыки.
                Нельзя менять `catalogVisible` и ручной порядок — это делает администратор.""")
public record PatchStudentMeReq(
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

        @Schema(description = "Текущий курс")
        CourseEnum course,

        @Schema(description = "Тип занятости")
        BusynessEnum busyness,

        @Schema(description = "Имя")
        @Size(min = 1, max = 255, message = "First name must be less than 255 characters")
        String firstName,

        @Schema(description = "Фамилия")
        @Size(min = 1, max = 255, message = "Last name must be less than 255 characters")
        String lastName,

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

        @Schema(description = "Список ID существующих навыков")
        List<@Positive Long> skillsIds,

        @Schema(description = "Согласие на показ на публичной витрине анонимам")
        Boolean publicProfileConsent,

        @Schema(description = "Отключить guided hints")
        Boolean hintsDisabled
) {
}
