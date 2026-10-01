package ru.ai.sin.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.io.FileNotFoundException;

import ru.ai.sin.exception.models.ApiException;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.ErrorResponse;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.exception.models.ProfileIncompleteErrorResponse;
import ru.ai.sin.exception.models.ProfileIncompleteException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                        HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                        "Authentication is required"
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        HttpStatus.FORBIDDEN.getReasonPhrase(),
                        "Access is denied"
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().isEmpty()
                ? ex.getMessage()
                : ex.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();

        log.warn("Validation failed: {}", msg);

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(new ErrorResponse(ex.getBody().getTitle(), msg));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex) {
        log.warn("Bad Request: code={}, status={}, msg={}", ex.getCode(), ex.getStatus(), ex.getMessage());

        return ResponseEntity
                .status(ex.getStatus())
                .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex) {
        log.warn("Not Found: code={}, status={}, msg={}", ex.getCode(), ex.getStatus(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "CONFLICT",
                        "Data constraint violation (e.g. duplicate key or foreign key)"
                ));
    }

    @ExceptionHandler(ProfileIncompleteException.class)
    public ResponseEntity<ProfileIncompleteErrorResponse> handleProfileIncomplete(ProfileIncompleteException ex) {
        log.warn("Profile incomplete: missingFields={}", ex.getMissingFields());
        return ResponseEntity
                .status(ex.getStatus())
                .body(new ProfileIncompleteErrorResponse(ex.getCode(), ex.getMessage(), ex.getMissingFields()));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        log.warn(
                "Api error: code={}, status={}, msg={}",
                ex.getCode(), ex.getStatus(), ex.getMessage()
        );

        return ResponseEntity
                .status(ex.getStatus())
                .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequestValidation(Exception ex) {
        log.warn("Request parse/validation error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        log.warn("Upload too large: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorResponse("UPLOAD_TOO_LARGE", "File is too large"));
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFileNotFound(FileNotFoundException ex) {
        log.warn("File not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(500)
                .body(new ErrorResponse("INTERNAL_ERROR", "Unexpected error occurred"));
    }
}
