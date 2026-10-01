package ru.ai.sin.exception.models;

import java.io.Serial;

public class NotFoundException extends ApiException {
    @Serial
    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(message, "NOT_FOUND", 404);
    }
}