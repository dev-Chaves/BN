package org.acme.domains.partnership.dto;

import org.acme.domains.partnership.PartnershipStatus;

import java.time.LocalDateTime;

public record PartnershipResponse(
        Long id,
        Long clientCompanyId,
        String clientCompanyName,
        Long benefitId,
        String benefitName,
        PartnershipStatus status,
        LocalDateTime createdAt
) {
}
