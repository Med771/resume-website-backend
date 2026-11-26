package ru.ai.sin.models.enums.convertor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.ai.sin.models.enums.RecruitmentRequestStatus;

@Converter(autoApply = true)
public class RecruitmentRequestStatusConverter implements AttributeConverter<RecruitmentRequestStatus, String> {

    @Override
    public String convertToDatabaseColumn(RecruitmentRequestStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public RecruitmentRequestStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : RecruitmentRequestStatus.fromCode(dbData);
    }
}
