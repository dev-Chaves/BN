package com.bnfix.ubm.domains.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeactivateCompanyRequest(
        @NotBlank(message = "Password cannot be empty")
        @Size(max = 72, message = "Password cannot exceed 72 characters")
        String password) {}
