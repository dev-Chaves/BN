package com.bn.benefix.benefit.dto;

import java.time.LocalDateTime;

public record BenefitCreationResponseDTO(
        long id,
        String benefitName,
        String nameProvider,
        boolean status,
        LocalDateTime createdAt
) {
}
