package ru.ai.sin.exception.models;

import lombok.Getter;

import java.io.Serial;

@Getter
public abstract class ApiException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String code;
    private final int status;

    protected ApiException(String message, String code, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

}