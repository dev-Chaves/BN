package com.bn.benefix.subscription.dto;

import java.time.LocalDateTime;

public record SubscriptionResponseDTO(
        Long id,
        String employeeName,
        String benefitName,
        LocalDateTime createdAt
) {}
