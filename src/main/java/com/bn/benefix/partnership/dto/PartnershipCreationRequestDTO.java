package com.bn.benefix.partnership.dto;

import jakarta.validation.constraints.NotNull;

public record PartnershipCreationRequestDTO(
        @NotNull(message = "Client Company ID cannot be null")
        Long clientCompanyId,
        @NotNull(message = "Benefit ID cannot be null")
        Long benefitId
) {
}
