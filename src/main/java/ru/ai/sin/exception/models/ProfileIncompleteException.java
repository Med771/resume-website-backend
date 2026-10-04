package ru.ai.sin.exception.models;

import org.springframework.http.HttpStatus;

import java.util.List;

public class ProfileIncompleteException extends ApiException {

    private final List<String> missingFields;

    public ProfileIncompleteException(List<String> missingFields) {
        super("Дозаполните профиль для доступа к переписке", "PROFILE_INCOMPLETE", HttpStatus.FORBIDDEN.value());
        this.missingFields = missingFields != null ? List.copyOf(missingFields) : List.of();
    }

    public List<String> getMissingFields() {
        return missingFields;
    }
}
