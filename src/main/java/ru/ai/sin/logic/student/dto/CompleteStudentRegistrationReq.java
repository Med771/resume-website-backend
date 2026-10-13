package ru.ai.sin.logic.student.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import ru.ai.sin.models.enums.CourseEnum;
import ru.ai.sin.validation.MinAge;

import java.time.LocalDate;

@Schema(name = "CompleteStudentRegistrationReq", description = "Завершение регистрации: дата рождения, курс и город")
public record CompleteStudentRegistrationReq(
        @Schema(description = "Дата рождения")
        @NotNull(message = "Укажите дату рождения")
        @JsonFormat(pattern = "yyyy-MM-dd")
        @Past(message = "Birth date must be in the past")
        @MinAge(14)
        LocalDate birthDate,

        @Schema(description = "Текущий курс")
        @NotNull(message = "Укажите курс")
        CourseEnum course,

        @Schema(description = "Город или кампус")
        @NotBlank(message = "Укажите город")
        @Size(min = 1, max = 255, message = "City must be less than 255 characters")
        String city
) {
}
