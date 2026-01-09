package com.bn.benefix.company.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CompanyCreationRequestDTO(
        @NotEmpty(message = "Name company cannot be null")
        String name,
        @NotNull(message = "CNPJ cannot be null")
        String cnpj
) {
}
