package ru.ai.sin.exception.models;

import java.io.Serial;

public class RateLimitExceededException extends ApiException {
    @Serial
    private static final long serialVersionUID = 1L;

    public RateLimitExceededException(String message) {
        super(message, "RATE_LIMIT", 429);
    }
}
