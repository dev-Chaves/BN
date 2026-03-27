package org.acme.domains.benefit.dto;

import java.time.LocalDateTime;

public record BenefitResponse(
        long id,
        String benefitName,
        String nameProvider,
        boolean status,
        LocalDateTime createdAt
) {
}
