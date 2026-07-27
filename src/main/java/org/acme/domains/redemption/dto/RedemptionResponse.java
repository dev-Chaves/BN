package org.acme.domains.redemption.dto;

import java.time.LocalDateTime;

public record RedemptionResponse(
        Long id,
        String benefitName,
        String beneficiaryName,
        LocalDateTime redeemedAt
) {}
