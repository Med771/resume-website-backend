package ru.ai.sin.models.enums.convertor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.ai.sin.models.enums.ChatMessageKind;

@Converter()
public class ChatMessageKindConverter implements AttributeConverter<ChatMessageKind, String> {

    @Override
    public String convertToDatabaseColumn(ChatMessageKind attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public ChatMessageKind convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        for (ChatMessageKind k : ChatMessageKind.values()) {
            if (k.getCode().equals(dbData)) {
                return k;
            }
        }
        throw new IllegalArgumentException("Unknown chat message kind: " + dbData);
    }
}
