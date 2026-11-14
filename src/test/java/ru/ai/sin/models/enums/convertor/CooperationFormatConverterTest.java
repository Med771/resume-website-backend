package ru.ai.sin.models.enums.convertor;

import org.junit.jupiter.api.Test;
import ru.ai.sin.models.enums.CooperationFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CooperationFormatConverterTest {

    private final CooperationFormatConverter converter = new CooperationFormatConverter();

    @Test
    void nullStaysNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void knownCodeRoundTrip() {
        assertThat(converter.convertToDatabaseColumn(CooperationFormat.INTERNSHIP)).isEqualTo("INTERNSHIP");
        assertThat(converter.convertToEntityAttribute("SUMMER_PRACTICE"))
                .isEqualTo(CooperationFormat.SUMMER_PRACTICE);
    }

    @Test
    void unknownCodeThrows() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("NOPE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
