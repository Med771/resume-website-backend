package ru.ai.sin.models.enums.convertor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.ai.sin.models.enums.VacancyApplicationStatus;

@Converter(autoApply = true)
public class VacancyApplicationStatusConverter implements AttributeConverter<VacancyApplicationStatus, String> {

    @Override
    public String convertToDatabaseColumn(VacancyApplicationStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public VacancyApplicationStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : VacancyApplicationStatus.fromCode(dbData);
    }
}
