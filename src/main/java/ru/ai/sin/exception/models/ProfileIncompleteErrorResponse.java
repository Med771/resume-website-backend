package ru.ai.sin.exception.models;

import java.util.List;

public record ProfileIncompleteErrorResponse(String code, String message, List<String> missingFields) {
}
