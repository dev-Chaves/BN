package org.acme.domains.redemption.dto;

import jakarta.validation.constraints.NotBlank;

public record RedemptionTokenRequest(
        @NotBlank(message = "Token cannot be blank") String token
) {}
