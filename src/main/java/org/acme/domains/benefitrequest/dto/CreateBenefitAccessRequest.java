package org.acme.domains.benefitrequest.dto;

import jakarta.validation.constraints.NotNull;

public record CreateBenefitAccessRequest(
        @NotNull(message = "Benefit ID cannot be null") Long benefitId
) {}
