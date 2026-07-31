package org.acme.domains.benefitrequest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateBenefitAccessRequest(
        @NotNull(message = "Benefit ID cannot be null") @Positive Long benefitId
) {}
