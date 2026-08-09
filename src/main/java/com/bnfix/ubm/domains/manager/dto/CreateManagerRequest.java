package com.bnfix.ubm.domains.manager.dto;

import jakarta.validation.constraints.*;

public record CreateManagerRequest(
        @NotBlank(message = "Name cannot be null") @Size(max = 120)
        String name,

        @NotBlank(message = "CPF cannot be null") String cpf,

        @NotBlank(message = "Email cannot be null") @Email @Size(max = 255)
        String email,

        @NotBlank(message = "Password cannot be null") @Size(min = 10, max = 72)
        String password,

        @NotNull(message = "Company ID cannot be null") @Positive
        Long companyId) {}
