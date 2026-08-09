package com.bnfix.ubm.domains.redemption.dto;

import java.time.LocalDateTime;

public record RedemptionPreviewResponse(
        boolean valid,
        String benefitName,
        String beneficiaryName,
        String providerName,
        LocalDateTime expiresAt,
        String message) {}
