package com.bn.benefix.manager.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ManagerCreationRequestDTO(
        @NotEmpty(message = "Name cannot be null")
        String name,
        @NotNull(message = "CPF cannot be null")
        String cpf,
        @NotEmpty(message = "Email cannot be null")
        String email,
        @NotEmpty(message = "Password cannot be null")
        String password,
        @NotNull(message = "Company ID cannot be null")
        Long companyId
) {
}
