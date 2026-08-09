package com.bnfix.ubm.domains.onboarding.dto;

import com.bnfix.ubm.domains.company.dto.CreateCompanyRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record OnboardingRequest(
        @Valid @NotNull CreateCompanyRequest company,
        @Valid @NotNull ManagerRegistrationData manager) {
    public record ManagerRegistrationData(
            @NotBlank(message = "Manager name cannot be null") @Size(max = 120)
            String name,

            @NotBlank(message = "Manager CPF cannot be null")
            String cpf,

            @NotBlank(message = "Email cannot be null") @Email @Size(max = 255)
            String email,

            @NotBlank(message = "Password cannot be null") @Size(min = 10, max = 72)
            String password) {}
}
