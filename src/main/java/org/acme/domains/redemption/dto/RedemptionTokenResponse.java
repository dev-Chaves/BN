package org.acme.domains.redemption.dto;

import java.time.LocalDateTime;

public record RedemptionTokenResponse(
        String token,
        String redemptionUrl,
        LocalDateTime expiresAt
) {}
