package ru.ai.sin.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MinAgeValidatorTest {

    private static Validator validator;

    private record Sample(@MinAge(14) LocalDate birthDate) {
    }

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void exactlyFourteenToday_valid() {
        assertThat(validator.validate(new Sample(LocalDate.now().minusYears(14)))).isEmpty();
    }

    @Test
    void thirteenYearsAndAlmostFourteen_invalid() {
        Set<ConstraintViolation<Sample>> violations =
                validator.validate(new Sample(LocalDate.now().minusYears(14).plusDays(1)));
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("14");
    }

    @Test
    void null_skipped() {
        assertThat(validator.validate(new Sample(null))).isEmpty();
    }
}
