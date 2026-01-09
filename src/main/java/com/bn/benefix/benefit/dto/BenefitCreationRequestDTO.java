package com.bn.benefix.benefit.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BenefitCreationRequestDTO(
        @NotEmpty(message = "Name cannot be null")
        String name,
        @NotEmpty(message = "Description cannot be null")
        String description,
        @NotNull(message = "Provider CNPJ cannot be null")
        String providerCNPJ
) {
}
