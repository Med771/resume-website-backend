package ru.ai.sin.models.enums.convertor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.ai.sin.models.enums.CooperationFormat;

@Converter(autoApply = true)
public class CooperationFormatConverter implements AttributeConverter<CooperationFormat, String> {

    @Override
    public String convertToDatabaseColumn(CooperationFormat attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public CooperationFormat convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return CooperationFormat.fromCode(dbData);
    }
}
