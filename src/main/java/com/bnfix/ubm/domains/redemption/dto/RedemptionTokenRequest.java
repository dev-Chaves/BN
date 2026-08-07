package com.bnfix.ubm.domains.redemption.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedemptionTokenRequest(@NotBlank(message = "Token cannot be blank") @Size(max = 128) String token) {}
