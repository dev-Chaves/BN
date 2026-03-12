package org.acme.domains.onboarding.dto;

import org.acme.domains.company.dto.CreateCompanyRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record OnboardingRequest(
        @Valid
        @NotNull
        CreateCompanyRequest company,
        
        @Valid
        @NotNull
        ManagerRegistrationData manager
) {
    public record ManagerRegistrationData(
            @NotEmpty(message = "Manager name cannot be null")
            String name,
            @NotNull(message = "Manager CPF cannot be null")
            String cpf,
            @NotEmpty(message = "Email cannot be null")
            String email,
            @NotEmpty(message = "Password cannot be null")
            String password
    ) {}
}
