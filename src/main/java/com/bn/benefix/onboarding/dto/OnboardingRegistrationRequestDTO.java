package com.bn.benefix.onboarding.dto;

import com.bn.benefix.company.dto.CompanyCreationRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record OnboardingRegistrationRequestDTO(
        @Valid
        @NotNull
        CompanyCreationRequestDTO company,
        
        @Valid
        @NotNull
        ManagerRegistrationData manager
) {
    public record ManagerRegistrationData(
            @NotEmpty(message = "Manager name cannot be null")
            String name,
            @NotNull(message = "Manager CPF cannot be null")
            String cpf
    ) {}
}
