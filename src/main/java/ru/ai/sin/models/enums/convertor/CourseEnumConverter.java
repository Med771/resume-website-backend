package ru.ai.sin.models.enums.convertor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import ru.ai.sin.models.enums.CourseEnum;

@Converter(autoApply = true)
public class CourseEnumConverter implements AttributeConverter<CourseEnum, String> {

    @Override
    public String convertToDatabaseColumn(CourseEnum attribute) {
        if (attribute == null) return null;
        return attribute.getCourse();
    }

    @Override
    public CourseEnum convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return CourseEnum.fromCourse(dbData);
    }
}