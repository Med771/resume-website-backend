package ru.ai.sin.models.enums.convertor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.ai.sin.models.enums.VacancyEmploymentTypeEnum;

@Converter(autoApply = true)
public class VacancyEmploymentTypeEnumConverter implements AttributeConverter<VacancyEmploymentTypeEnum, String> {

    @Override
    public String convertToDatabaseColumn(VacancyEmploymentTypeEnum attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public VacancyEmploymentTypeEnum convertToEntityAttribute(String dbData) {
        return dbData == null ? null : VacancyEmploymentTypeEnum.fromCode(dbData);
    }
}
