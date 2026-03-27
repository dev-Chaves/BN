package org.acme.domains.company.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateCompanyRequest(
        @NotEmpty(message = "Name company cannot be null")
        String name,
        @NotNull(message = "CNPJ cannot be null")
        String cnpj
) {
}
