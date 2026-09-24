package com.bnfix.ubm.domains.enrollment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnrollmentRequest(
        @NotBlank(message = "Name cannot be null") @Size(max = 120)
        String name,

        @NotBlank(message = "CPF cannot be null") String cpf,

        @NotBlank(message = "Email cannot be null") @Email @Size(max = 255)
        String email,

        @NotBlank(message = "Password cannot be null") @Size(min = 10, max = 72)
        String password) {}
