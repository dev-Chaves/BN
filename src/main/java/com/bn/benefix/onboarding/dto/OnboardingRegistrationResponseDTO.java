package com.bn.benefix.onboarding.dto;

import com.bn.benefix.company.dto.CompanyCreationResponseDTO;
import com.bn.benefix.management.dto.ManagerCreationResponseDTO;

public record OnboardingRegistrationResponseDTO(
        String cnpj,
        String company,
        String manager
) {
}

