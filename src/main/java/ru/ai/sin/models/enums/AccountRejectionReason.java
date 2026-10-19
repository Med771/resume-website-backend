package ru.ai.sin.models.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccountRejectionReason {
    INCOMPLETE_PROFILE("INCOMPLETE_PROFILE", "Анкета заполнена недостаточно"),
    INVALID_DATA("INVALID_DATA", "Данные или контакты выглядят некорректно"),
    NOT_ELIGIBLE("NOT_ELIGIBLE", "Аккаунт не подходит под правила площадки"),
    DUPLICATE("DUPLICATE", "Такой аккаунт уже есть"),
    OTHER("OTHER", null);

    private final String code;
    private final String formulation;

    /**
     * Текст причины для письма и кабинета. Для {@link #OTHER} это комментарий администратора.
     */
    public String formulation(String comment) {
        if (this == OTHER) {
            return comment == null ? "" : comment.trim();
        }
        return formulation;
    }

    public static AccountRejectionReason fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Укажите причину отказа");
        }
        String c = raw.trim().toUpperCase();
        for (AccountRejectionReason v : values()) {
            if (v.code.equals(c)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Неизвестная причина отказа");
    }
}
