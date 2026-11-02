package ru.ai.sin.models.enums.convertor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.ai.sin.models.enums.JobSearchStatus;

@Converter(autoApply = true)
public class JobSearchStatusConverter implements AttributeConverter<JobSearchStatus, String> {
    @Override
    public String convertToDatabaseColumn(JobSearchStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public JobSearchStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return JobSearchStatus.fromCode(dbData);
    }
}
