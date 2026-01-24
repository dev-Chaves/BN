package com.bn.benefix.onboarding.dto;

public record OnboardingRegistrationResponseDTO(
        String cnpj,
        String company,
        String manager
) {
}

