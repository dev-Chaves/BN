package com.bnfix.ubm.api;

import java.time.OffsetDateTime;

public record ApiError(String message, int status, OffsetDateTime timestamp) {
}
