package org.acme.domains.partnership.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePartnershipRequest(
        @NotNull(message = "Benefit ID cannot be null") @Positive
        Long benefitId
) {
}
