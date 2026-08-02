package org.acme.domains.auth.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SwitchCompanyRequest(
        @NotNull(message = "Company ID cannot be null")
        @Positive(message = "Company ID must be positive")
        Long companyId
) {
}
