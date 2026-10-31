package ru.ai.sin.models.enums.convertor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.ai.sin.models.enums.ExperienceType;

@Converter()
public class ExperienceTypeConverter implements AttributeConverter<ExperienceType, String> {

    @Override
    public String convertToDatabaseColumn(ExperienceType attribute) {
        return attribute == null ? ExperienceType.WORK.getCode() : attribute.getCode();
    }

    @Override
    public ExperienceType convertToEntityAttribute(String dbData) {
        return ExperienceType.fromCode(dbData);
    }
}
