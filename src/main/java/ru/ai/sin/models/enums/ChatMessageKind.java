package ru.ai.sin.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChatMessageKind {
    USER("user"),
    SYSTEM("system");

    private final String code;
}
