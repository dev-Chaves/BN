package com.bn.benefix.partnership.dto;

import com.bn.benefix.partnership.PartnershipStatus;

public record PartnershipUpdateRequestDTO(
        PartnershipStatus status
) {
}
