package org.acme.domains.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequest(
        @NotBlank(message = "Company name cannot be null") @Size(max = 160)
        String name
) {
}
