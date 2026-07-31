package org.acme.domains.shared.api;

import jakarta.validation.ConstraintViolationException;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.acme.domains.auth.AuthenticationException;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;
import java.util.concurrent.CompletionException;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        Throwable cause = unwrapInfrastructureException(exception);
        if (cause instanceof ConstraintViolationException validationException) {
            String message = validationException.getConstraintViolations().stream()
                    .map(v -> v.getMessage())
                    .collect(Collectors.joining(", "));
            return response(Response.Status.BAD_REQUEST, message);
        }

        if (cause instanceof AuthenticationException) {
            return response(Response.Status.UNAUTHORIZED, "Invalid email or password");
        }

        if (cause instanceof IllegalArgumentException || cause instanceof IllegalStateException) {
            return response(Response.Status.BAD_REQUEST, cause.getMessage());
        }

        if (cause instanceof BadRequestException) {
            return response(Response.Status.BAD_REQUEST, fallbackMessage(cause.getMessage(), "Invalid request body"));
        }

        if (cause instanceof SecurityException) {
            return response(Response.Status.FORBIDDEN, fallbackMessage(cause.getMessage(), "Access denied"));
        }

        if (cause instanceof NotFoundException) {
            return response(Response.Status.NOT_FOUND, cause.getMessage());
        }

        if (cause instanceof NotSupportedException) {
            return response(Response.Status.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type: " + cause.getMessage());
        }

        if (cause instanceof org.hibernate.exception.ConstraintViolationException hibernateConstraint) {
            LOG.warnf("Database constraint violation: %s", hibernateConstraint.getMessage());
            return response(Response.Status.CONFLICT, "Resource already exists or conflicts with existing data");
        }

        LOG.errorf(
                exception,
                "Unhandled application exception type=%s message=%s",
                exception.getClass().getName(),
                fallbackMessage(exception.getMessage(), "<empty>")
        );
        return response(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

    private Throwable unwrapInfrastructureException(Throwable exception) {
        Throwable current = exception;
        while (!(current instanceof org.hibernate.exception.ConstraintViolationException)
                && (current instanceof CompletionException || current instanceof PersistenceException)
                && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private String fallbackMessage(String message, String fallback) {
        if (message == null || message.isBlank()) {
            return fallback;
        }
        return message;
    }

    private Response response(Response.Status status, String message) {
        return Response.status(status)
                .entity(new ErrorResponse(message, status.getStatusCode(), OffsetDateTime.now()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
