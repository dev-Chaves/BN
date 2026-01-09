package com.bn.benefix.employee.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record EmployeeCreationRequestDTO(
        @NotEmpty(message = "Name cannot be null")
        String name,
        @NotNull(message = "CPF cannot be null")
        String cpf,
        @NotNull(message = "Company ID cannot be null")
        Long companyId
) {
}
