package ru.ai.sin.exception.models;

import java.io.Serial;

public class BadRequestException extends ApiException {
    @Serial
    private static final long serialVersionUID = 1L;

    public BadRequestException(String message) {
        super(message, "BAD_REQUEST", 400);
    }
}