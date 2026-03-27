package org.acme.domains.subscription.dto;

import java.time.LocalDateTime;

public record SubscriptionResponse(
        Long id,
        String employeeName,
        String benefitName,
        LocalDateTime createdAt
) {
}
