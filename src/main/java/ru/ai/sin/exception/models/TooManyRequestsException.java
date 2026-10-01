package ru.ai.sin.exception.models;

import java.io.Serial;

public class TooManyRequestsException extends ApiException {
    @Serial
    private static final long serialVersionUID = 1L;

    public TooManyRequestsException(String message) {
        super(message, "TOO_MANY_REQUESTS", 429);
    }
}
