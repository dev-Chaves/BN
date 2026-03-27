package org.acme.domains.partnership.dto;

import org.acme.domains.partnership.PartnershipStatus;

import java.time.LocalDateTime;

public record PartnershipResponse(
        Long id,
        Long clientCompanyId,
        Long benefitId,
        PartnershipStatus status,
        LocalDateTime createdAt
) {
}
