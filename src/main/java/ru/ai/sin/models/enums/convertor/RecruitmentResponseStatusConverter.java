package ru.ai.sin.models.enums.convertor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.ai.sin.models.enums.RecruitmentResponseStatus;

@Converter(autoApply = true)
public class RecruitmentResponseStatusConverter implements AttributeConverter<RecruitmentResponseStatus, String> {

    @Override
    public String convertToDatabaseColumn(RecruitmentResponseStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public RecruitmentResponseStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : RecruitmentResponseStatus.fromCode(dbData);
    }
}
