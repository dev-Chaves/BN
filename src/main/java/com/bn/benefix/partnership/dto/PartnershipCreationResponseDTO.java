package com.bn.benefix.partnership.dto;

import com.bn.benefix.partnership.PartnershipStatus;

import java.time.LocalDateTime;

public record PartnershipCreationResponseDTO(
        Long id,
        Long clientCompanyId,
        Long benefitId,
        PartnershipStatus status,
        LocalDateTime createdAt
) {
}
