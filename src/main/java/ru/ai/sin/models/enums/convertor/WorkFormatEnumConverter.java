package ru.ai.sin.models.enums.convertor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.ai.sin.models.enums.WorkFormatEnum;

@Converter(autoApply = true)
public class WorkFormatEnumConverter implements AttributeConverter<WorkFormatEnum, String> {

    @Override
    public String convertToDatabaseColumn(WorkFormatEnum attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public WorkFormatEnum convertToEntityAttribute(String dbData) {
        return dbData == null ? null : WorkFormatEnum.fromCode(dbData);
    }
}
