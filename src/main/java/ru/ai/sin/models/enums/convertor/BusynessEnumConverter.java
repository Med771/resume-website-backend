package ru.ai.sin.models.enums.convertor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.ai.sin.models.enums.BusynessEnum;

@Converter(autoApply = true)
public class BusynessEnumConverter implements AttributeConverter<BusynessEnum, String> {
    @Override
    public String convertToDatabaseColumn(BusynessEnum attribute) {
        if (attribute == null) return null;
        return attribute.getBusyness();
    }

    @Override
    public BusynessEnum convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return BusynessEnum.fromBusyness(dbData);
    }
}
