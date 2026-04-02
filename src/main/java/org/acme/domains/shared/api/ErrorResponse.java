package org.acme.domains.shared.api;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.OffsetDateTime;

@RegisterForReflection
public record ErrorResponse(
        String message,
        int status,
        OffsetDateTime timestamp
) {
}
