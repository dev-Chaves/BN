package com.bnfix.ubm.api;

import com.bnfix.ubm.domains.auth.AuthenticationException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> authentication(AuthenticationException exception) {
        log.warn("Authentication failed: {}", exception.getMessage());
        return error(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    ResponseEntity<ApiError> security(SecurityException exception) {
        log.warn("Access denied: {}", exception.getMessage());
        return error(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Invalid request body");
        log.warn("Request validation failed: {}", message);
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> constraintViolation(ConstraintViolationException exception) {
        log.warn("Constraint violation: {}", exception.getMessage());
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> illegalArgument(IllegalArgumentException exception) {
        log.warn("Illegal argument: {}", exception.getMessage());
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler({EntityNotFoundException.class, NoSuchElementException.class})
    ResponseEntity<ApiError> notFound(RuntimeException exception) {
        log.warn("Resource not found: {}", exception.getMessage());
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflict(DataIntegrityViolationException exception) {
        log.warn("Data integrity conflict: {}", exception.getMessage());
        return error(HttpStatus.CONFLICT, "Resource conflicts with existing data");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception exception) {
        log.error("Unhandled exception", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> illegal(Exception exception) {
        log.error("Illegal state for the method: " + exception.getMessage());

        return error(HttpStatus.BAD_REQUEST, "Invalid state");
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> statusError(ResponseStatusException exception) {
        log.warn("""
                Request failed with: ,
                status: "%s",
                message: "%s"
                """.formatted(exception.getStatusCode(), exception.getMessage()));

        return error((HttpStatus) exception.getStatusCode(), exception.getReason());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> methodNotSupported(HttpRequestMethodNotSupportedException exception) {
        log.warn("HTTP Method not supported to this operation: " + exception.getMethod() + exception.getMessage());

        return error(HttpStatus.METHOD_NOT_ALLOWED, "Method no supported");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiError(
                        message == null ? status.getReasonPhrase() : message, status.value(), OffsetDateTime.now()));
    }
}
