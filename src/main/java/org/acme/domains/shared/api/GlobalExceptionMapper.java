package org.acme.domains.shared.api;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof ConstraintViolationException validationException) {
            String message = validationException.getConstraintViolations().stream()
                    .map(v -> v.getMessage())
                    .collect(Collectors.joining(", "));
            return response(Response.Status.BAD_REQUEST, message);
        }

        if (exception instanceof IllegalArgumentException || exception instanceof IllegalStateException) {
            return response(Response.Status.BAD_REQUEST, exception.getMessage());
        }

        if (exception instanceof SecurityException) {
            return response(Response.Status.FORBIDDEN, exception.getMessage());
        }

        if (exception instanceof NotFoundException) {
            return response(Response.Status.NOT_FOUND, exception.getMessage());
        }

        return response(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

    private Response response(Response.Status status, String message) {
        return Response.status(status)
                .entity(new ErrorResponse(message, status.getStatusCode(), OffsetDateTime.now()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
