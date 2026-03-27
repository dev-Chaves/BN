package org.acme.domains.partnership.dto;

import jakarta.validation.constraints.NotNull;

public record CreatePartnershipRequest(
        @NotNull(message = "Benefit ID cannot be null")
        Long benefitId
) {
}
