package org.acme.domains.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCompanyRequest(
        @NotBlank(message = "Name company cannot be null") @Size(max = 160)
        String name,
        @NotBlank(message = "CNPJ cannot be null")
        String cnpj
) {
}
