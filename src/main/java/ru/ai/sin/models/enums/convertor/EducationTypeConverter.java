package ru.ai.sin.models.enums.convertor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.ai.sin.models.enums.EducationType;

@Converter
public class EducationTypeConverter implements AttributeConverter<EducationType, String> {

    @Override
    public String convertToDatabaseColumn(EducationType attribute) {
        return attribute == null ? EducationType.STATE.getCode() : attribute.getCode();
    }

    @Override
    public EducationType convertToEntityAttribute(String dbData) {
        return EducationType.fromCode(dbData);
    }
}
