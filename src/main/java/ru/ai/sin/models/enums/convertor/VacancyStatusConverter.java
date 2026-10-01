package ru.ai.sin.models.enums.convertor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.ai.sin.models.enums.VacancyStatus;

@Converter(autoApply = true)
public class VacancyStatusConverter implements AttributeConverter<VacancyStatus, String> {

    @Override
    public String convertToDatabaseColumn(VacancyStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public VacancyStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : VacancyStatus.fromCode(dbData);
    }
}
