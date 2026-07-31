package org.acme.domains.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(
        @NotBlank(message = "Name cannot be null") @Size(max = 120)
        String name,
        @NotBlank(message = "CPF cannot be null")
        String cpf,
        @NotBlank(message = "Email cannot be empty") @Email @Size(max = 255)
        String email,
        @NotBlank(message = "Password cannot be empty") @Size(min = 10, max = 72)
        String password,
        @NotNull(message = "Company ID cannot be null") @Positive
        Long companyId
) {
}
